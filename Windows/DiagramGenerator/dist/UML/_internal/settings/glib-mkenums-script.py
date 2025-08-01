

# If the code below looks horrible and unpythonic, do not panic.
#
# It is.
#
# This is a manual conversion from the original Perl script to
# Python. Improvements are welcome.
#
from __future__ import print_function, unicode_literals

import argparse
import os
import re
import sys
import tempfile
import io
import errno
import codecs
import locale

# Non-english locale systems might complain to unrecognized character
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')

VERSION_STR = '''glib-mkenums version 2.84.2
glib-mkenums comes with ABSOLUTELY NO WARRANTY.
You may redistribute copies of glib-mkenums under the terms of
the GNU General Public License which can be found in the
GLib source package. Sources, examples and contact
information are available at http://www.gtk.org'''

# pylint: disable=too-few-public-methods
class Color:
    '''ANSI Terminal colors'''
    GREEN = '\033[1;32m'
    BLUE = '\033[1;34m'
    YELLOW = '\033[1;33m'
    RED = '\033[1;31m'
    END = '\033[0m'


def print_color(msg, color=Color.END, prefix='MESSAGE'):
    '''Print a string with a color prefix'''
    if os.isatty(sys.stderr.fileno()):
        real_prefix = '{start}{prefix}{end}'.format(start=color, prefix=prefix, end=Color.END)
    else:
        real_prefix = prefix
    print('{prefix}: {msg}'.format(prefix=real_prefix, msg=msg), file=sys.stderr)


def print_error(msg):
    '''Print an error, and terminate'''
    print_color(msg, color=Color.RED, prefix='ERROR')
    sys.exit(1)


def print_warning(msg, fatal=False):
    '''Print a warning, and optionally terminate'''
    if fatal:
        color = Color.RED
        prefix = 'ERROR'
    else:
        color = Color.YELLOW
        prefix = 'WARNING'
    print_color(msg, color, prefix)
    if fatal:
        sys.exit(1)


def print_info(msg):
    '''Print a message'''
    print_color(msg, color=Color.GREEN, prefix='INFO')


def get_rspfile_args(rspfile):
    '''
    Response files are useful on Windows where there is a command-line character
    limit of 8191 because when passing sources as arguments to glib-mkenums this
    limit can be exceeded in large codebases.

    There is no specification for response files and each tool that supports it
    generally writes them out in slightly different ways, but some sources are:
    https://docs.microsoft.com/en-us/visualstudio/msbuild/msbuild-response-files
    https://docs.microsoft.com/en-us/windows/desktop/midl/the-response-file-command
    '''
    import shlex
    if not os.path.isfile(rspfile):
        sys.exit('Response file {!r} does not exist'.format(rspfile))
    try:
        with open(rspfile, 'r') as f:
            cmdline = f.read()
    except OSError as e:
        sys.exit('Response file {!r} could not be read: {}'
                 .format(rspfile, e.strerror))
    return shlex.split(cmdline)


def write_output(output):
    global output_stream
    print(output, file=output_stream)


# Python 2 defaults to ASCII in case stdout is redirected.
# This should make it match Python 3, which uses the locale encoding.
if sys.stdout.encoding is None:
    output_stream = codecs.getwriter(
        locale.getpreferredencoding())(sys.stdout)
else:
    output_stream = sys.stdout


# Some source files aren't UTF-8 and the old perl version didn't care.
# Replace invalid data with a replacement character to keep things working.
# https://bugzilla.gnome.org/show_bug.cgi?id=785113#c20
def replace_and_warn(err):
    # 7 characters of context either side of the offending character
    print_warning('UnicodeWarning: {} at {} ({})'.format(
        err.reason, err.start,
        err.object[err.start - 7:err.end + 7]))
    return ('?', err.end)

codecs.register_error('replace_and_warn', replace_and_warn)


# glib-mkenums.py
# Information about the current enumeration
flags = None # Is enumeration a bitmask?
option_underscore_name = '' # Overridden underscore variant of the enum name
                            # for example to fix the cases we don't get the
                            # mixed-case -> underscorized transform right.
option_lowercase_name = ''  # DEPRECATED.  A lower case name to use as part
                            # of the *_get_type() function, instead of the
                            # one that we guess. For instance, when an enum
                            # uses abnormal capitalization and we can not
                            # guess where to put the underscores.
option_since = ''           # User provided version info for the enum.
seenbitshift = 0            # Have we seen bitshift operators?
seenprivate = False         # Have we seen a private option?
enum_prefix = None          # Prefix for this enumeration
enumname = ''               # Name for this enumeration
enumshort = ''              # $enumname without prefix
enumname_prefix = ''        # prefix of $enumname
enumindex = 0               # Global enum counter
firstenum = 1               # Is this the first enumeration per file?
entries = []                # [ name, val ] for each entry
c_namespace = {}            # C symbols namespace.

output = ''                 # Filename to write result into

def parse_trigraph(opts):
    result = {}
    for opt in re.findall(r'(?:[^\s,"]|"(?:\\.|[^"])*")+', opts):
        opt = re.sub(r'^\s*', '', opt)
        opt = re.sub(r'\s*$', '', opt)
        m = re.search(r'(\w+)(?:=(.+))?', opt)
        assert m is not None
        groups = m.groups()
        key = groups[0]
        if len(groups) > 1:
            val = groups[1]
        else:
            val = 1
        result[key] = val.strip('"') if val is not None else None
    return result

def parse_entries(file, file_name):
    global entries, enumindex, enumname, seenbitshift, seenprivate, flags
    looking_for_name = False

    while True:
        line = file.readline()
        if not line:
            break

        line = line.strip()

        # read lines until we have no open comments
        while re.search(r'/\*([^*]|\*(?!/))*$', line):
            line += file.readline()

        # strip comments w/o options
        line = re.sub(r'''/\*(?!<)
            ([^*]+|\*(?!/))*
           \*/''', '', line, flags=re.X)

        line = line.rstrip()

        # skip empty lines
        if len(line.strip()) == 0:
            continue

        if looking_for_name:
            m = re.match(r'\s*(\w+)', line)
            if m:
                enumname = m.group(1)
                return True

        # Handle include files
        m = re.match(r'\#include\s*<([^>]*)>', line)
        if m:
            newfilename = os.path.join("..", m.group(1))
            newfile = io.open(newfilename, encoding="utf-8",
                              errors="replace_and_warn")

            if not parse_entries(newfile, newfilename):
                return False
            else:
                continue

        m = re.match(r'\s*\}\s*(\w+)', line)
        if m:
            enumname = m.group(1)
            enumindex += 1
            return 1

        m = re.match(r'\s*\}', line)
        if m:
            enumindex += 1
            looking_for_name = True
            continue

        m = re.match(r'''\s*
              (\w+)\s*                   # name
              (\s+[A-Z]+_(?:AVAILABLE|DEPRECATED)_ENUMERATOR_IN_[0-9_]+(?:_FOR\s*\(\s*\w+\s*\))?\s*)?    # availability
              (?:=(                      # value
                   \s*'[^']*'\s*          # char
                     |                      # OR
                   \s*\w+\s*\(.*\)\s*       # macro with multiple args
                   |                        # OR
                   (?:[^,/]|/(?!\*))*       # anything but a comma or comment
                  ))?,?\s*
              (?:/\*<                    # options
                (([^*]|\*(?!/))*)
               >\s*\*/)?,?
              \s*$''', line, flags=re.X)
        if m:
            groups = m.groups()
            name = groups[0]
            availability = None
            value = None
            options = None
            if len(groups) > 1:
                availability = groups[1]
            if len(groups) > 2:
                value = groups[2]
            if len(groups) > 3:
                options = groups[3]
            if flags is None and value is not None and '<<' in value:
                seenbitshift = 1

            if options is not None:
                options = parse_trigraph(options)
                if 'skip' not in options:
                    entries.append((name, value, seenprivate, options.get('nick')))
            else:
                entries.append((name, value, seenprivate))
        else:
            m = re.match(r'''\s*
                         /\*< (([^*]|\*(?!/))*) >\s*\*/
                         \s*$''', line, flags=re.X)
            if m:
                options = m.groups()[0]
                if options is not None:
                    options = parse_trigraph(options)
                    if 'private' in options:
                        seenprivate = True
                        continue
                    if 'public' in options:
                        seenprivate = False
                        continue
            if re.match(r's*\#', line):
                pass
            else:
                print_warning('Failed to parse "{}" in {}'.format(line, file_name))
    return False

help_epilog = '''Production text substitutions:
  \u0040EnumName\u0040            PrefixTheXEnum
  \u0040enum_name\u0040           prefix_the_xenum
  \u0040ENUMNAME\u0040            PREFIX_THE_XENUM
  \u0040ENUMSHORT\u0040           THE_XENUM
  \u0040ENUMPREFIX\u0040          PREFIX
  \u0040enumsince\u0040           the user-provided since value given
  \u0040VALUENAME\u0040           PREFIX_THE_XVALUE
  \u0040valuenick\u0040           the-xvalue
  \u0040valuenum\u0040            the integer value (limited support, Since: 2.26)
  \u0040type\u0040                either enum or flags
  \u0040Type\u0040                either Enum or Flags
  \u0040TYPE\u0040                either ENUM or FLAGS
  \u0040filename\u0040            name of current input file
  \u0040basename\u0040            base name of the current input file (Since: 2.22)
'''


# production variables:
idprefix = ""    # "G", "Gtk", etc
symprefix = ""   # "g", "gtk", etc, if not just lc($idprefix)
fhead = ""   # output file header
fprod = ""   # per input file production
ftail = ""   # output file trailer
eprod = ""   # per enum text (produced prior to value itarations)
vhead = ""   # value header, produced before iterating over enum values
vprod = ""   # value text, produced for each enum value
vtail = ""   # value tail, produced after iterating over enum values
comment_tmpl = ""   # comment template

def read_template_file(file):
    global idprefix, symprefix, fhead, fprod, ftail, eprod, vhead, vprod, vtail, comment_tmpl
    tmpl = {'file-header': fhead,
            'file-production': fprod,
            'file-tail': ftail,
            'enumeration-production': eprod,
            'value-header': vhead,
            'value-production': vprod,
            'value-tail': vtail,
            'comment': comment_tmpl,
           }
    in_ = 'junk'

    ifile = io.open(file, encoding="utf-8", errors="replace_and_warn")
    for line in ifile:
        m = re.match(r'\/\*\*\*\s+(BEGIN|END)\s+([\w-]+)\s+\*\*\*\/', line)
        if m:
            if in_ == 'junk' and m.group(1) == 'BEGIN' and m.group(2) in tmpl:
                in_ = m.group(2)
                continue
            elif in_ == m.group(2) and m.group(1) == 'END' and m.group(2) in tmpl:
                in_ = 'junk'
                continue
            else:
                sys.exit("Malformed template file " + file)

        if in_ != 'junk':
            tmpl[in_] += line

    if in_ != 'junk':
        sys.exit("Malformed template file " + file)

    fhead = tmpl['file-header']
    fprod = tmpl['file-production']
    ftail = tmpl['file-tail']
    eprod = tmpl['enumeration-production']
    vhead = tmpl['value-header']
    vprod = tmpl['value-production']
    vtail = tmpl['value-tail']
    comment_tmpl = tmpl['comment']

parser = argparse.ArgumentParser(epilog=help_epilog,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)

parser.add_argument('--identifier-prefix', default='', dest='idprefix',
                    help='Identifier prefix')
parser.add_argument('--symbol-prefix', default='', dest='symprefix',
                    help='Symbol prefix')
parser.add_argument('--fhead', default=[], dest='fhead', action='append',
                    help='Output file header')
parser.add_argument('--ftail', default=[], dest='ftail', action='append',
                    help='Output file footer')
parser.add_argument('--fprod', default=[], dest='fprod', action='append',
                    help='Put out TEXT every time a new input file is being processed.')
parser.add_argument('--eprod', default=[], dest='eprod', action='append',
                    help='Per enum text, produced prior to value iterations')
parser.add_argument('--vhead', default=[], dest='vhead', action='append',
                    help='Value header, produced before iterating over enum values')
parser.add_argument('--vprod', default=[], dest='vprod', action='append',
                    help='Value text, produced for each enum value.')
parser.add_argument('--vtail', default=[], dest='vtail', action='append',
                    help='Value tail, produced after iterating over enum values')
parser.add_argument('--comments', default='', dest='comment_tmpl',
                    help='Comment structure')
parser.add_argument('--template', default='', dest='template',
                    help='Template file')
parser.add_argument('--output', default=None, dest='output')
parser.add_argument('--version', '-v', default=False, action='store_true', dest='version',
                    help='Print version information')
parser.add_argument('args', nargs='*',
                    help='One or more input files, or a single argument @rspfile_path '
                    'pointing to a file that contains the actual arguments')

# Support reading an rspfile of the form @filename which contains the args
# to be parsed
if len(sys.argv) == 2 and sys.argv[1].startswith('@'):
    args = get_rspfile_args(sys.argv[1][1:])
else:
    args = sys.argv[1:]

options = parser.parse_args(args)

if options.version:
    print(VERSION_STR)
    sys.exit(0)

def unescape_cmdline_args(arg):
    arg = arg.replace('\\n', '\n')
    arg = arg.replace('\\r', '\r')
    return arg.replace('\\t', '\t')

if options.template != '':
    read_template_file(options.template)

idprefix += options.idprefix
symprefix += options.symprefix

# This is a hack to maintain some semblance of backward compatibility with
# the old, Perl-based glib-mkenums. The old tool had an implicit ordering
# on the arguments and templates; each argument was parsed in order, and
# all the strings appended. This allowed developers to write:
#
#   glib-mkenums \
#     --fhead ... \
#     --template a-template-file.c.in \
#     --ftail ...
#
# And have the fhead be prepended to the file-head stanza in the template,
# as well as the ftail be appended to the file-tail stanza in the template.
# Short of throwing away ArgumentParser and going over sys.argv[] element
# by element, we can simulate that behaviour by ensuring some ordering in
# how we build the template strings:
#
#   - the head stanzas are always prepended to the template
#   - the prod stanzas are always appended to the template
#   - the tail stanzas are always appended to the template
#
# Within each instance of the command line argument, we append each value
# to the array in the order in which it appears on the command line.
fhead = ''.join([unescape_cmdline_args(x) for x in options.fhead]) + fhead
vhead = ''.join([unescape_cmdline_args(x) for x in options.vhead]) + vhead

fprod += ''.join([unescape_cmdline_args(x) for x in options.fprod])
eprod += ''.join([unescape_cmdline_args(x) for x in options.eprod])
vprod += ''.join([unescape_cmdline_args(x) for x in options.vprod])

ftail = ftail + ''.join([unescape_cmdline_args(x) for x in options.ftail])
vtail = vtail + ''.join([unescape_cmdline_args(x) for x in options.vtail])

if options.comment_tmpl != '':
    comment_tmpl = unescape_cmdline_args(options.comment_tmpl)
elif comment_tmpl == "":
    # default to C-style comments
    comment_tmpl = "/* \u0040comment\u0040 */"

output = options.output

if output is not None:
    (out_dir, out_fn) = os.path.split(options.output)
    out_suffix = '_' + os.path.splitext(out_fn)[1]
    if out_dir == '':
        out_dir = '.'
    fd, filename = tempfile.mkstemp(dir=out_dir)
    os.close(fd)
    tmpfile = io.open(filename, "w", encoding="utf-8")
    output_stream = tmpfile
else:
    tmpfile = None

# put auto-generation comment
comment = comment_tmpl.replace('\u0040comment\u0040',
                               'This file is generated by glib-mkenums, do '
                               'not modify it. This code is licensed under '
                               'the same license as the containing project. '
                               'Note that it links to GLib, so must comply '
                               'with the LGPL linking clauses.')
write_output("\n" + comment + '\n')

def replace_specials(prod):
    prod = prod.replace(r'\\a', r'\a')
    prod = prod.replace(r'\\b', r'\b')
    prod = prod.replace(r'\\t', r'\t')
    prod = prod.replace(r'\\n', r'\n')
    prod = prod.replace(r'\\f', r'\f')
    prod = prod.replace(r'\\r', r'\r')
    prod = prod.rstrip()
    return prod


def warn_if_filename_basename_used(section, prod):
    for substitution in ('\u0040filename\u0040',
                         '\u0040basename\u0040'):
        if substitution in prod:
            print_warning('{} used in {} section.'.format(substitution,
                                                          section))

if len(fhead) > 0:
    prod = fhead
    warn_if_filename_basename_used('file-header', prod)
    prod = replace_specials(prod)
    write_output(prod)

def process_file(curfilename):
    global entries, flags, seenbitshift, seenprivate, enum_prefix, c_namespace
    firstenum = True

    try:
        curfile = io.open(curfilename, encoding="utf-8",
                          errors="replace_and_warn")
    except IOError as e:
        if e.errno == errno.ENOENT:
            print_warning('No file "{}" found.'.format(curfilename))
            return
        raise

    while True:
        line = curfile.readline()
        if not line:
            break

        line = line.strip()

        # read lines until we have no open comments
        while re.search(r'/\*([^*]|\*(?!/))*$', line):
            line += curfile.readline()

        # strip comments w/o options
        line = re.sub(r'''/\*(?!<)
           ([^*]+|\*(?!/))*
           \*/''', '', line)

        # ignore forward declarations
        if re.match(r'\s*typedef\s+enum.*;', line):
            continue

        m = re.match(r'''\s*typedef\s+enum\s*[_A-Za-z]*[_A-Za-z0-9]*\s*
               ({)?\s*
               (?:/\*<
                 (([^*]|\*(?!/))*)
                >\s*\*/)?
               \s*({)?''', line, flags=re.X)
        if m:
            groups = m.groups()
            if len(groups) >= 2 and groups[1] is not None:
                options = parse_trigraph(groups[1])
                if 'skip' in options:
                    continue
                enum_prefix = options.get('prefix', None)
                flags = options.get('flags', None)
                if 'flags' in options:
                    if flags is None:
                        flags = 1
                    else:
                        flags = int(flags)
                option_lowercase_name = options.get('lowercase_name', None)
                option_underscore_name = options.get('underscore_name', None)
                option_since = options.get('since', None)
            else:
                enum_prefix = None
                flags = None
                option_lowercase_name = None
                option_underscore_name = None
                option_since = None

            if option_lowercase_name is not None:
                if option_underscore_name is not None:
                    print_warning("lowercase_name overridden with underscore_name")
                    option_lowercase_name = None
                else:
                    print_warning("lowercase_name is deprecated, use underscore_name")

            # Didn't have trailing '{' look on next lines
            if groups[0] is None and (len(groups) < 4 or groups[3] is None):
                while True:
                    line = curfile.readline()
                    if not line:
                        print_error("Syntax error when looking for opening { in enum")
                    if re.match(r'\s*\{', line):
                        break

            seenbitshift = 0
            seenprivate = False
            entries = []

            # Now parse the entries
            parse_entries(curfile, curfilename)

            # figure out if this was a flags or enums enumeration
            if flags is None:
                flags = seenbitshift

            # Autogenerate a prefix
            if enum_prefix is None:
                for entry in entries:
                    if not entry[2] and (len(entry) < 4 or entry[3] is None):
                        name = entry[0]
                        if enum_prefix is not None:
                            enum_prefix = os.path.commonprefix([name, enum_prefix])
                        else:
                            enum_prefix = name
                if enum_prefix is None:
                    enum_prefix = ""
                else:
                    # Trim so that it ends in an underscore
                    enum_prefix = re.sub(r'_[^_]*$', '_', enum_prefix)
            else:
                # canonicalize user defined prefixes
                enum_prefix = enum_prefix.upper()
                enum_prefix = enum_prefix.replace('-', '_')
                enum_prefix = re.sub(r'(.*)([^_])$', r'\1\2_', enum_prefix)

            fixed_entries = []
            for e in entries:
                name = e[0]
                num = e[1]
                private = e[2]
                if len(e) < 4 or e[3] is None:
                    nick = re.sub(r'^' + enum_prefix, '', name)
                    nick = nick.replace('_', '-').lower()
                    e = (name, num, private, nick)
                fixed_entries.append(e)
            entries = fixed_entries

            # Spit out the output
            if option_underscore_name is not None:
                enumlong = option_underscore_name.upper()
                enumsym = option_underscore_name.lower()
                enumshort = re.sub(r'^[A-Z][A-Z0-9]*_', '', enumlong)

                enumname_prefix = re.sub('_' + enumshort + '$', '', enumlong)
            elif symprefix == '' and idprefix == '':
                # enumname is e.g. GMatchType
                enspace = re.sub(r'^([A-Z][a-z]*).*$', r'\1', enumname)

                enumshort = re.sub(r'^[A-Z][a-z]*', '', enumname)
                enumshort = re.sub(r'([^A-Z])([A-Z])', r'\1_\2', enumshort)
                enumshort = re.sub(r'([A-Z][A-Z])([A-Z][0-9a-z])', r'\1_\2', enumshort)
                enumshort = enumshort.upper()

                enumname_prefix = re.sub(r'^([A-Z][a-z]*).*$', r'\1', enumname).upper()

                enumlong = enspace.upper() + "_" + enumshort
                enumsym = enspace.lower() + "_" + enumshort.lower()

                if option_lowercase_name is not None:
                    enumsym = option_lowercase_name
            else:
                enumshort = enumname
                if idprefix:
                    enumshort = re.sub(r'^' + idprefix, '', enumshort)
                else:
                    enumshort = re.sub(r'/^[A-Z][a-z]*', '', enumshort)

                enumshort = re.sub(r'([^A-Z])([A-Z])', r'\1_\2', enumshort)
                enumshort = re.sub(r'([A-Z][A-Z])([A-Z][0-9a-z])', r'\1_\2', enumshort)
                enumshort = enumshort.upper()

                if symprefix:
                    enumname_prefix = symprefix.upper()
                else:
                    enumname_prefix = idprefix.upper()

                enumlong = enumname_prefix + "_" + enumshort
                enumsym = enumlong.lower()

            if option_since is not None:
                enumsince = option_since
            else:
                enumsince = ""

            if firstenum:
                firstenum = False

                if len(fprod) > 0:
                    prod = fprod
                    base = os.path.basename(curfilename)

                    prod = prod.replace('\u0040filename\u0040', curfilename)
                    prod = prod.replace('\u0040basename\u0040', base)
                    prod = replace_specials(prod)

                    write_output(prod)

            if len(eprod) > 0:
                prod = eprod

                prod = prod.replace('\u0040enum_name\u0040', enumsym)
                prod = prod.replace('\u0040EnumName\u0040', enumname)
                prod = prod.replace('\u0040ENUMSHORT\u0040', enumshort)
                prod = prod.replace('\u0040ENUMNAME\u0040', enumlong)
                prod = prod.replace('\u0040ENUMPREFIX\u0040', enumname_prefix)
                prod = prod.replace('\u0040enumsince\u0040', enumsince)
                if flags:
                    prod = prod.replace('\u0040type\u0040', 'flags')
                else:
                    prod = prod.replace('\u0040type\u0040', 'enum')
                if flags:
                    prod = prod.replace('\u0040Type\u0040', 'Flags')
                else:
                    prod = prod.replace('\u0040Type\u0040', 'Enum')
                if flags:
                    prod = prod.replace('\u0040TYPE\u0040', 'FLAGS')
                else:
                    prod = prod.replace('\u0040TYPE\u0040', 'ENUM')
                prod = replace_specials(prod)
                write_output(prod)

            if len(vhead) > 0:
                prod = vhead
                prod = prod.replace('\u0040enum_name\u0040', enumsym)
                prod = prod.replace('\u0040EnumName\u0040', enumname)
                prod = prod.replace('\u0040ENUMSHORT\u0040', enumshort)
                prod = prod.replace('\u0040ENUMNAME\u0040', enumlong)
                prod = prod.replace('\u0040ENUMPREFIX\u0040', enumname_prefix)
                prod = prod.replace('\u0040enumsince\u0040', enumsince)
                if flags:
                    prod = prod.replace('\u0040type\u0040', 'flags')
                else:
                    prod = prod.replace('\u0040type\u0040', 'enum')
                if flags:
                    prod = prod.replace('\u0040Type\u0040', 'Flags')
                else:
                    prod = prod.replace('\u0040Type\u0040', 'Enum')
                if flags:
                    prod = prod.replace('\u0040TYPE\u0040', 'FLAGS')
                else:
                    prod = prod.replace('\u0040TYPE\u0040', 'ENUM')
                prod = replace_specials(prod)
                write_output(prod)

            if len(vprod) > 0:
                prod = vprod
                next_num = 0

                prod = replace_specials(prod)
                for name, num, private, nick in entries:
                    tmp_prod = prod

                    if '\u0040valuenum\u0040' in prod:
                        # only attempt to eval the value if it is requested
                        # this prevents us from throwing errors otherwise
                        if num is not None:
                            # use sandboxed evaluation as a reasonable
                            # approximation to C constant folding
                            inum = eval(num, {}, c_namespace)

                            # Support character literals
                            if isinstance(inum, str) and len(inum) == 1:
                                inum = ord(inum)

                            # make sure it parsed to an integer
                            if not isinstance(inum, int):
                                sys.exit("Unable to parse enum value '%s'" % num)
                            num = inum
                        else:
                            num = next_num

                        c_namespace[name] = num
                        tmp_prod = tmp_prod.replace('\u0040valuenum\u0040', str(num))
                        next_num = int(num) + 1

                    if private:
                        continue

                    tmp_prod = tmp_prod.replace('\u0040VALUENAME\u0040', name)
                    tmp_prod = tmp_prod.replace('\u0040valuenick\u0040', nick)
                    if flags:
                        tmp_prod = tmp_prod.replace('\u0040type\u0040', 'flags')
                    else:
                        tmp_prod = tmp_prod.replace('\u0040type\u0040', 'enum')
                    if flags:
                        tmp_prod = tmp_prod.replace('\u0040Type\u0040', 'Flags')
                    else:
                        tmp_prod = tmp_prod.replace('\u0040Type\u0040', 'Enum')
                    if flags:
                        tmp_prod = tmp_prod.replace('\u0040TYPE\u0040', 'FLAGS')
                    else:
                        tmp_prod = tmp_prod.replace('\u0040TYPE\u0040', 'ENUM')
                    tmp_prod = tmp_prod.rstrip()

                    write_output(tmp_prod)

            if len(vtail) > 0:
                prod = vtail
                prod = prod.replace('\u0040enum_name\u0040', enumsym)
                prod = prod.replace('\u0040EnumName\u0040', enumname)
                prod = prod.replace('\u0040ENUMSHORT\u0040', enumshort)
                prod = prod.replace('\u0040ENUMNAME\u0040', enumlong)
                prod = prod.replace('\u0040ENUMPREFIX\u0040', enumname_prefix)
                prod = prod.replace('\u0040enumsince\u0040', enumsince)
                if flags:
                    prod = prod.replace('\u0040type\u0040', 'flags')
                else:
                    prod = prod.replace('\u0040type\u0040', 'enum')
                if flags:
                    prod = prod.replace('\u0040Type\u0040', 'Flags')
                else:
                    prod = prod.replace('\u0040Type\u0040', 'Enum')
                if flags:
                    prod = prod.replace('\u0040TYPE\u0040', 'FLAGS')
                else:
                    prod = prod.replace('\u0040TYPE\u0040', 'ENUM')
                prod = replace_specials(prod)
                write_output(prod)

for fname in sorted(options.args):
    process_file(fname)

if len(ftail) > 0:
    prod = ftail
    warn_if_filename_basename_used('file-tail', prod)
    prod = replace_specials(prod)
    write_output(prod)

# put auto-generation comment
comment = comment_tmpl
comment = comment.replace('\u0040comment\u0040', 'Generated data ends here')
write_output("\n" + comment + "\n")

if tmpfile is not None:
    tmpfilename = tmpfile.name
    tmpfile.close()

    try:
        os.unlink(options.output)
    except OSError as error:
        if error.errno != errno.ENOENT:
            raise error

    os.rename(tmpfilename, options.output)
