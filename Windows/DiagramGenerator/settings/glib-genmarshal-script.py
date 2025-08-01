

# pylint: disable=too-many-lines, missing-docstring, invalid-name

# This file is part of GLib
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, see <http://www.gnu.org/licenses/>.

import argparse
import os
import re
import sys

VERSION_STR = '''glib-genmarshal version 2.84.2
glib-genmarshal comes with ABSOLUTELY NO WARRANTY.
You may redistribute copies of glib-genmarshal under the terms of
the GNU General Public License which can be found in the
GLib source package. Sources, examples and contact
information are available at http://www.gtk.org'''

GETTERS_STR = '''#ifdef G_ENABLE_DEBUG
#define g_marshal_value_peek_boolean(v)  g_value_get_boolean (v)
#define g_marshal_value_peek_char(v)     g_value_get_schar (v)
#define g_marshal_value_peek_uchar(v)    g_value_get_uchar (v)
#define g_marshal_value_peek_int(v)      g_value_get_int (v)
#define g_marshal_value_peek_uint(v)     g_value_get_uint (v)
#define g_marshal_value_peek_long(v)     g_value_get_long (v)
#define g_marshal_value_peek_ulong(v)    g_value_get_ulong (v)
#define g_marshal_value_peek_int64(v)    g_value_get_int64 (v)
#define g_marshal_value_peek_uint64(v)   g_value_get_uint64 (v)
#define g_marshal_value_peek_enum(v)     g_value_get_enum (v)
#define g_marshal_value_peek_flags(v)    g_value_get_flags (v)
#define g_marshal_value_peek_float(v)    g_value_get_float (v)
#define g_marshal_value_peek_double(v)   g_value_get_double (v)
#define g_marshal_value_peek_string(v)   (char*) g_value_get_string (v)
#define g_marshal_value_peek_param(v)    g_value_get_param (v)
#define g_marshal_value_peek_boxed(v)    g_value_get_boxed (v)
#define g_marshal_value_peek_pointer(v)  g_value_get_pointer (v)
#define g_marshal_value_peek_object(v)   g_value_get_object (v)
#define g_marshal_value_peek_variant(v)  g_value_get_variant (v)
#else /* !G_ENABLE_DEBUG */
/* WARNING: This code accesses GValues directly, which is UNSUPPORTED API.
 *          Do not access GValues directly in your code. Instead, use the
 *          g_value_get_*() functions
 */
#define g_marshal_value_peek_boolean(v)  (v)->data[0].v_int
#define g_marshal_value_peek_char(v)     (v)->data[0].v_int
#define g_marshal_value_peek_uchar(v)    (v)->data[0].v_uint
#define g_marshal_value_peek_int(v)      (v)->data[0].v_int
#define g_marshal_value_peek_uint(v)     (v)->data[0].v_uint
#define g_marshal_value_peek_long(v)     (v)->data[0].v_long
#define g_marshal_value_peek_ulong(v)    (v)->data[0].v_ulong
#define g_marshal_value_peek_int64(v)    (v)->data[0].v_int64
#define g_marshal_value_peek_uint64(v)   (v)->data[0].v_uint64
#define g_marshal_value_peek_enum(v)     (v)->data[0].v_long
#define g_marshal_value_peek_flags(v)    (v)->data[0].v_ulong
#define g_marshal_value_peek_float(v)    (v)->data[0].v_float
#define g_marshal_value_peek_double(v)   (v)->data[0].v_double
#define g_marshal_value_peek_string(v)   (v)->data[0].v_pointer
#define g_marshal_value_peek_param(v)    (v)->data[0].v_pointer
#define g_marshal_value_peek_boxed(v)    (v)->data[0].v_pointer
#define g_marshal_value_peek_pointer(v)  (v)->data[0].v_pointer
#define g_marshal_value_peek_object(v)   (v)->data[0].v_pointer
#define g_marshal_value_peek_variant(v)  (v)->data[0].v_pointer
#endif /* !G_ENABLE_DEBUG */'''

DEPRECATED_MSG_STR = 'The token "{}" is deprecated; use "{}" instead'

VA_ARG_STR = \
    '  arg{:d} = ({:s}) va_arg (args_copy, {:s});'
STATIC_CHECK_STR = \
    '(param_types[{:d}] & G_SIGNAL_TYPE_STATIC_SCOPE) == 0 && '
BOX_TYPED_STR = \
    '    arg{idx:d} = {box_func} (param_types[{idx:d}] & ~G_SIGNAL_TYPE_STATIC_SCOPE, arg{idx:d});'
BOX_UNTYPED_STR = \
    '    arg{idx:d} = {box_func} (arg{idx:d});'
UNBOX_TYPED_STR = \
    '    {unbox_func} (param_types[{idx:d}] & ~G_SIGNAL_TYPE_STATIC_SCOPE, arg{idx:d});'
UNBOX_UNTYPED_STR = \
    '    {unbox_func} (arg{idx:d});'

STD_PREFIX = 'g_cclosure_marshal'

# These are part of our ABI; keep this in sync with gmarshal.h
GOBJECT_MARSHALLERS = {
    'g_cclosure_marshal_VOID__VOID',
    'g_cclosure_marshal_VOID__BOOLEAN',
    'g_cclosure_marshal_VOID__CHAR',
    'g_cclosure_marshal_VOID__UCHAR',
    'g_cclosure_marshal_VOID__INT',
    'g_cclosure_marshal_VOID__UINT',
    'g_cclosure_marshal_VOID__LONG',
    'g_cclosure_marshal_VOID__ULONG',
    'g_cclosure_marshal_VOID__ENUM',
    'g_cclosure_marshal_VOID__FLAGS',
    'g_cclosure_marshal_VOID__FLOAT',
    'g_cclosure_marshal_VOID__DOUBLE',
    'g_cclosure_marshal_VOID__STRING',
    'g_cclosure_marshal_VOID__PARAM',
    'g_cclosure_marshal_VOID__BOXED',
    'g_cclosure_marshal_VOID__POINTER',
    'g_cclosure_marshal_VOID__OBJECT',
    'g_cclosure_marshal_VOID__VARIANT',
    'g_cclosure_marshal_VOID__UINT_POINTER',
    'g_cclosure_marshal_BOOLEAN__FLAGS',
    'g_cclosure_marshal_STRING__OBJECT_POINTER',
    'g_cclosure_marshal_BOOLEAN__BOXED_BOXED',
}


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
    sys.stderr.write('{prefix}: {msg}\n'.format(prefix=real_prefix, msg=msg))


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


def generate_licensing_comment(outfile):
    outfile.write('/* This file is generated by glib-genmarshal, do not '
                  'modify it. This code is licensed under the same license as '
                  'the containing project. Note that it links to GLib, so '
                  'must comply with the LGPL linking clauses. */\n')


def generate_header_preamble(outfile, prefix='', std_includes=True, use_pragma=False):
    '''Generate the preamble for the marshallers header file'''
    generate_licensing_comment(outfile)

    if use_pragma:
        outfile.write('#pragma once\n')
        outfile.write('\n')
    else:
        outfile.write('#ifndef __{}_MARSHAL_H__\n'.format(prefix.upper()))
        outfile.write('#define __{}_MARSHAL_H__\n'.format(prefix.upper()))
        outfile.write('\n')
    # Maintain compatibility with the old C-based tool
    if std_includes:
        outfile.write('#include <glib-object.h>\n')
        outfile.write('\n')

    outfile.write('G_BEGIN_DECLS\n')
    outfile.write('\n')


def generate_header_postamble(outfile, prefix='', use_pragma=False):
    '''Generate the postamble for the marshallers header file'''
    outfile.write('\n')
    outfile.write('G_END_DECLS\n')

    if not use_pragma:
        outfile.write('\n')
        outfile.write('#endif /* __{}_MARSHAL_H__ */\n'.format(prefix.upper()))


def generate_body_preamble(outfile, std_includes=True, include_headers=None, cpp_defines=None, cpp_undefines=None):
    '''Generate the preamble for the marshallers source file'''
    generate_licensing_comment(outfile)

    for header in (include_headers or []):
        outfile.write('#include "{}"\n'.format(header))
    if include_headers:
        outfile.write('\n')

    for define in (cpp_defines or []):
        s = define.split('=')
        symbol = s[0]
        value = s[1] if len(s) > 1 else '1'
        outfile.write('#define {} {}\n'.format(symbol, value))
    if cpp_defines:
        outfile.write('\n')

    for undefine in (cpp_undefines or []):
        outfile.write('#undef {}\n'.format(undefine))
    if cpp_undefines:
        outfile.write('\n')

    if std_includes:
        outfile.write('#include <glib-object.h>\n')
        outfile.write('\n')

    outfile.write(GETTERS_STR)
    outfile.write('\n\n')


# Marshaller arguments, as a dictionary where the key is the token used in
# the source file, and the value is another dictionary with the following
# keys:
#
#   - signal: the token used in the marshaller prototype (mandatory)
#   - ctype: the C type for the marshaller argument (mandatory)
#   - getter: the function used to retrieve the argument from the GValue
#       array when invoking the callback (optional)
#   - promoted: the C type used by va_arg() to retrieve the argument from
#       the va_list when invoking the callback (optional, only used when
#       generating va_list marshallers)
#   - box: an array of two elements, containing the boxing and unboxing
#       functions for the given type (optional, only used when generating
#       va_list marshallers)
#   - static-check: a boolean value, if the given type should perform
#       a static type check before boxing or unboxing the argument (optional,
#       only used when generating va_list marshallers)
#   - takes-type: a boolean value, if the boxing and unboxing functions
#       for the given type require the type (optional, only used when
#       generating va_list marshallers)
#   - deprecated: whether the token has been deprecated (optional)
#   - replaced-by: the token used to replace a deprecated token (optional,
#       only used if deprecated is True)
IN_ARGS = {
    'VOID': {
        'signal': 'VOID',
        'ctype': 'void',
    },
    'BOOLEAN': {
        'signal': 'BOOLEAN',
        'ctype': 'gboolean',
        'getter': 'g_marshal_value_peek_boolean',
    },
    'CHAR': {
        'signal': 'CHAR',
        'ctype': 'gchar',
        'promoted': 'gint',
        'getter': 'g_marshal_value_peek_char',
    },
    'UCHAR': {
        'signal': 'UCHAR',
        'ctype': 'guchar',
        'promoted': 'guint',
        'getter': 'g_marshal_value_peek_uchar',
    },
    'INT': {
        'signal': 'INT',
        'ctype': 'gint',
        'getter': 'g_marshal_value_peek_int',
    },
    'UINT': {
        'signal': 'UINT',
        'ctype': 'guint',
        'getter': 'g_marshal_value_peek_uint',
    },
    'LONG': {
        'signal': 'LONG',
        'ctype': 'glong',
        'getter': 'g_marshal_value_peek_long',
    },
    'ULONG': {
        'signal': 'ULONG',
        'ctype': 'gulong',
        'getter': 'g_marshal_value_peek_ulong',
    },
    'INT64': {
        'signal': 'INT64',
        'ctype': 'gint64',
        'getter': 'g_marshal_value_peek_int64',
    },
    'UINT64': {
        'signal': 'UINT64',
        'ctype': 'guint64',
        'getter': 'g_marshal_value_peek_uint64',
    },
    'ENUM': {
        'signal': 'ENUM',
        'ctype': 'gint',
        'getter': 'g_marshal_value_peek_enum',
    },
    'FLAGS': {
        'signal': 'FLAGS',
        'ctype': 'guint',
        'getter': 'g_marshal_value_peek_flags',
    },
    'FLOAT': {
        'signal': 'FLOAT',
        'ctype': 'gfloat',
        'promoted': 'gdouble',
        'getter': 'g_marshal_value_peek_float',
    },
    'DOUBLE': {
        'signal': 'DOUBLE',
        'ctype': 'gdouble',
        'getter': 'g_marshal_value_peek_double',
    },
    'STRING': {
        'signal': 'STRING',
        'ctype': 'gpointer',
        'getter': 'g_marshal_value_peek_string',
        'box': ['g_strdup', 'g_free'],
        'static-check': True,
    },
    'PARAM': {
        'signal': 'PARAM',
        'ctype': 'gpointer',
        'getter': 'g_marshal_value_peek_param',
        'box': ['g_param_spec_ref', 'g_param_spec_unref'],
        'static-check': True,
    },
    'BOXED': {
        'signal': 'BOXED',
        'ctype': 'gpointer',
        'getter': 'g_marshal_value_peek_boxed',
        'box': ['g_boxed_copy', 'g_boxed_free'],
        'static-check': True,
        'takes-type': True,
    },
    'POINTER': {
        'signal': 'POINTER',
        'ctype': 'gpointer',
        'getter': 'g_marshal_value_peek_pointer',
    },
    'OBJECT': {
        'signal': 'OBJECT',
        'ctype': 'gpointer',
        'getter': 'g_marshal_value_peek_object',
        'box': ['g_object_ref', 'g_object_unref'],
    },
    'VARIANT': {
        'signal': 'VARIANT',
        'ctype': 'gpointer',
        'getter': 'g_marshal_value_peek_variant',
        'box': ['g_variant_ref_sink', 'g_variant_unref'],
        'static-check': True,
        'takes-type': False,
    },

    # Deprecated tokens
    'NONE': {
        'signal': 'VOID',
        'ctype': 'void',
        'deprecated': True,
        'replaced_by': 'VOID'
    },
    'BOOL': {
        'signal': 'BOOLEAN',
        'ctype': 'gboolean',
        'getter': 'g_marshal_value_peek_boolean',
        'deprecated': True,
        'replaced_by': 'BOOLEAN'
    }
}


# Marshaller return values, as a dictionary where the key is the token used
# in the source file, and the value is another dictionary with the following
# keys:
#
#   - signal: the token used in the marshaller prototype (mandatory)
#   - ctype: the C type for the marshaller argument (mandatory)
#   - setter: the function used to set the return value of the callback
#       into a GValue (optional)
#   - deprecated: whether the token has been deprecated (optional)
#   - replaced-by: the token used to replace a deprecated token (optional,
#       only used if deprecated is True)
OUT_ARGS = {
    'VOID': {
        'signal': 'VOID',
        'ctype': 'void',
    },
    'BOOLEAN': {
        'signal': 'BOOLEAN',
        'ctype': 'gboolean',
        'setter': 'g_value_set_boolean',
    },
    'CHAR': {
        'signal': 'CHAR',
        'ctype': 'gchar',
        'setter': 'g_value_set_char',
    },
    'UCHAR': {
        'signal': 'UCHAR',
        'ctype': 'guchar',
        'setter': 'g_value_set_uchar',
    },
    'INT': {
        'signal': 'INT',
        'ctype': 'gint',
        'setter': 'g_value_set_int',
    },
    'UINT': {
        'signal': 'UINT',
        'ctype': 'guint',
        'setter': 'g_value_set_uint',
    },
    'LONG': {
        'signal': 'LONG',
        'ctype': 'glong',
        'setter': 'g_value_set_long',
    },
    'ULONG': {
        'signal': 'ULONG',
        'ctype': 'gulong',
        'setter': 'g_value_set_ulong',
    },
    'INT64': {
        'signal': 'INT64',
        'ctype': 'gint64',
        'setter': 'g_value_set_int64',
    },
    'UINT64': {
        'signal': 'UINT64',
        'ctype': 'guint64',
        'setter': 'g_value_set_uint64',
    },
    'ENUM': {
        'signal': 'ENUM',
        'ctype': 'gint',
        'setter': 'g_value_set_enum',
    },
    'FLAGS': {
        'signal': 'FLAGS',
        'ctype': 'guint',
        'setter': 'g_value_set_flags',
    },
    'FLOAT': {
        'signal': 'FLOAT',
        'ctype': 'gfloat',
        'setter': 'g_value_set_float',
    },
    'DOUBLE': {
        'signal': 'DOUBLE',
        'ctype': 'gdouble',
        'setter': 'g_value_set_double',
    },
    'STRING': {
        'signal': 'STRING',
        'ctype': 'gchar*',
        'setter': 'g_value_take_string',
    },
    'PARAM': {
        'signal': 'PARAM',
        'ctype': 'GParamSpec*',
        'setter': 'g_value_take_param',
    },
    'BOXED': {
        'signal': 'BOXED',
        'ctype': 'gpointer',
        'setter': 'g_value_take_boxed',
    },
    'POINTER': {
        'signal': 'POINTER',
        'ctype': 'gpointer',
        'setter': 'g_value_set_pointer',
    },
    'OBJECT': {
        'signal': 'OBJECT',
        'ctype': 'GObject*',
        'setter': 'g_value_take_object',
    },
    'VARIANT': {
        'signal': 'VARIANT',
        'ctype': 'GVariant*',
        'setter': 'g_value_take_variant',
    },

    # Deprecated tokens
    'NONE': {
        'signal': 'VOID',
        'ctype': 'void',
        'setter': None,
        'deprecated': True,
        'replaced_by': 'VOID',
    },
    'BOOL': {
        'signal': 'BOOLEAN',
        'ctype': 'gboolean',
        'setter': 'g_value_set_boolean',
        'deprecated': True,
        'replaced_by': 'BOOLEAN',
    },
}


def check_args(retval, params, fatal_warnings=False):
    '''Check the @retval and @params tokens for invalid and deprecated symbols.'''
    if retval not in OUT_ARGS:
        print_error('Unknown return value type "{}"'.format(retval))

    if OUT_ARGS[retval].get('deprecated', False):
        replaced_by = OUT_ARGS[retval]['replaced_by']
        print_warning(DEPRECATED_MSG_STR.format(retval, replaced_by), fatal_warnings)

    for param in params:
        if param not in IN_ARGS:
            print_error('Unknown parameter type "{}"'.format(param))
        else:
            if IN_ARGS[param].get('deprecated', False):
                replaced_by = IN_ARGS[param]['replaced_by']
                print_warning(DEPRECATED_MSG_STR.format(param, replaced_by), fatal_warnings)


def indent(text, level=0, fill=' '):
    '''Indent @text by @level columns, using the @fill character'''
    return ''.join([fill for x in range(level)]) + text


# pylint: disable=too-few-public-methods
class Visibility:
    '''Symbol visibility options'''
    NONE = 0
    INTERNAL = 1
    EXTERN = 2


def generate_marshaller_name(prefix, retval, params, replace_deprecated=True):
    '''Generate a marshaller name for the given @prefix, @retval, and @params.
    If @replace_deprecated is True, the generated name will replace deprecated
    tokens.'''
    if replace_deprecated:
        real_retval = OUT_ARGS[retval]['signal']
        real_params = []
        for param in params:
            real_params.append(IN_ARGS[param]['signal'])
    else:
        real_retval = retval
        real_params = params
    return '{prefix}_{retval}__{args}'.format(prefix=prefix,
                                              retval=real_retval,
                                              args='_'.join(real_params))


def generate_prototype(retval, params,
                       prefix='g_cclosure_user_marshal',
                       visibility=Visibility.NONE,
                       va_marshal=False):
    '''Generate a marshaller declaration with the given @visibility. If @va_marshal
    is True, the marshaller will use variadic arguments in place of a GValue array.'''
    signature = []

    if visibility == Visibility.INTERNAL:
        signature += ['G_GNUC_INTERNAL']
    elif visibility == Visibility.EXTERN:
        signature += ['extern']

    function_name = generate_marshaller_name(prefix, retval, params)

    if not va_marshal:
        signature += ['void ' + function_name + ' (GClosure     *closure,']
        width = len('void ') + len(function_name) + 2

        signature += [indent('GValue       *return_value,', level=width, fill=' ')]
        signature += [indent('guint         n_param_values,', level=width, fill=' ')]
        signature += [indent('const GValue *param_values,', level=width, fill=' ')]
        signature += [indent('gpointer      invocation_hint,', level=width, fill=' ')]
        signature += [indent('gpointer      marshal_data);', level=width, fill=' ')]
    else:
        signature += ['void ' + function_name + 'v (GClosure *closure,']
        width = len('void ') + len(function_name) + 3

        signature += [indent('GValue   *return_value,', level=width, fill=' ')]
        signature += [indent('gpointer  instance,', level=width, fill=' ')]
        signature += [indent('va_list   args,', level=width, fill=' ')]
        signature += [indent('gpointer  marshal_data,', level=width, fill=' ')]
        signature += [indent('int       n_params,', level=width, fill=' ')]
        signature += [indent('GType    *param_types);', level=width, fill=' ')]

    return signature


# pylint: disable=too-many-statements, too-many-locals, too-many-branches
def generate_body(retval, params, prefix, va_marshal=False):
    '''Generate a marshaller definition. If @va_marshal is True, the marshaller
    will use va_list and variadic arguments in place of a GValue array.'''
    retval_setter = OUT_ARGS[retval].get('setter', None)
    # If there's no return value then we can mark the retval argument as unused
    # and get a minor optimisation, as well as avoid a compiler warning
    if not retval_setter:
        unused = ' G_GNUC_UNUSED'
    else:
        unused = ''

    body = ['void']

    function_name = generate_marshaller_name(prefix, retval, params)

    if not va_marshal:
        body += [function_name + ' (GClosure     *closure,']
        width = len(function_name) + 2

        body += [indent('GValue       *return_value{},'.format(unused), level=width, fill=' ')]
        body += [indent('guint         n_param_values,', level=width, fill=' ')]
        body += [indent('const GValue *param_values,', level=width, fill=' ')]
        body += [indent('gpointer      invocation_hint G_GNUC_UNUSED,', level=width, fill=' ')]
        body += [indent('gpointer      marshal_data)', level=width, fill=' ')]
    else:
        body += [function_name + 'v (GClosure *closure,']
        width = len(function_name) + 3

        body += [indent('GValue   *return_value{},'.format(unused), level=width, fill=' ')]
        body += [indent('gpointer  instance,', level=width, fill=' ')]
        body += [indent('va_list   args,', level=width, fill=' ')]
        body += [indent('gpointer  marshal_data,', level=width, fill=' ')]
        body += [indent('int       n_params,', level=width, fill=' ')]
        body += [indent('GType    *param_types)', level=width, fill=' ')]

    # Filter the arguments that have a getter
    get_args = [x for x in params if IN_ARGS[x].get('getter', None) is not None]

    body += ['{']

    # Generate the type of the marshaller function
    typedef_marshal = generate_marshaller_name('GMarshalFunc', retval, params)

    typedef = '  typedef {ctype} (*{func_name}) ('.format(ctype=OUT_ARGS[retval]['ctype'],
                                                          func_name=typedef_marshal)
    pad = len(typedef)
    typedef += 'gpointer data1,'
    body += [typedef]

    for idx, in_arg in enumerate(get_args):
        body += [indent('{} arg{:d},'.format(IN_ARGS[in_arg]['ctype'], idx + 1), level=pad)]

    body += [indent('gpointer data2);', level=pad)]

    # Variable declarations
    body += ['  GCClosure *cc = (GCClosure *) closure;']
    body += ['  gpointer data1, data2;']
    body += ['  {} callback;'.format(typedef_marshal)]

    if retval_setter:
        body += ['  {} v_return;'.format(OUT_ARGS[retval]['ctype'])]

    if va_marshal:
        for idx, arg in enumerate(get_args):
            body += ['  {} arg{:d};'.format(IN_ARGS[arg]['ctype'], idx)]

        if get_args:
            body += ['  va_list args_copy;']
            body += ['']

            body += ['  va_copy (args_copy, args);']

            for idx, arg in enumerate(get_args):
                ctype = IN_ARGS[arg]['ctype']
                promoted_ctype = IN_ARGS[arg].get('promoted', ctype)
                body += [VA_ARG_STR.format(idx, ctype, promoted_ctype)]
                if IN_ARGS[arg].get('box', None):
                    box_func = IN_ARGS[arg]['box'][0]
                    if IN_ARGS[arg].get('static-check', False):
                        static_check = STATIC_CHECK_STR.format(idx)
                    else:
                        static_check = ''
                    arg_check = 'arg{:d} != NULL'.format(idx)
                    body += ['  if ({}{})'.format(static_check, arg_check)]
                    if IN_ARGS[arg].get('takes-type', False):
                        body += [BOX_TYPED_STR.format(idx=idx, box_func=box_func)]
                    else:
                        body += [BOX_UNTYPED_STR.format(idx=idx, box_func=box_func)]

            body += ['  va_end (args_copy);']

    body += ['']

    # Preconditions check
    if retval_setter:
        body += ['  g_return_if_fail (return_value != NULL);']

    if not va_marshal:
        body += ['  g_return_if_fail (n_param_values == {:d});'.format(len(get_args) + 1)]

    body += ['']

    # Marshal instance, data, and callback set up
    body += ['  if (G_CCLOSURE_SWAP_DATA (closure))']
    body += ['    {']
    body += ['      data1 = closure->data;']
    if va_marshal:
        body += ['      data2 = instance;']
    else:
        body += ['      data2 = g_value_peek_pointer (param_values + 0);']
    body += ['    }']
    body += ['  else']
    body += ['    {']
    if va_marshal:
        body += ['      data1 = instance;']
    else:
        body += ['      data1 = g_value_peek_pointer (param_values + 0);']
    body += ['      data2 = closure->data;']
    body += ['    }']
    # pylint: disable=line-too-long
    body += ['  callback = ({}) (marshal_data ? marshal_data : cc->callback);'.format(typedef_marshal)]
    body += ['']

    # Marshal callback action
    if retval_setter:
        callback = ' {} callback ('.format(' v_return =')
    else:
        callback = '  callback ('

    pad = len(callback)
    body += [callback + 'data1,']

    if va_marshal:
        for idx, arg in enumerate(get_args):
            body += [indent('arg{:d},'.format(idx), level=pad)]
    else:
        for idx, arg in enumerate(get_args):
            arg_getter = IN_ARGS[arg]['getter']
            body += [indent('{} (param_values + {:d}),'.format(arg_getter, idx + 1), level=pad)]

    body += [indent('data2);', level=pad)]

    if va_marshal:
        boxed_args = [x for x in get_args if IN_ARGS[x].get('box', None) is not None]
        if not boxed_args:
            body += ['']
        else:
            for idx, arg in enumerate(get_args):
                if not IN_ARGS[arg].get('box', None):
                    continue
                unbox_func = IN_ARGS[arg]['box'][1]
                if IN_ARGS[arg].get('static-check', False):
                    static_check = STATIC_CHECK_STR.format(idx)
                else:
                    static_check = ''
                arg_check = 'arg{:d} != NULL'.format(idx)
                body += ['  if ({}{})'.format(static_check, arg_check)]
                if IN_ARGS[arg].get('takes-type', False):
                    body += [UNBOX_TYPED_STR.format(idx=idx, unbox_func=unbox_func)]
                else:
                    body += [UNBOX_UNTYPED_STR.format(idx=idx, unbox_func=unbox_func)]

    if retval_setter:
        body += ['']
        body += ['  {} (return_value, v_return);'.format(retval_setter)]

    body += ['}']

    return body


def generate_marshaller_alias(outfile, marshaller, real_marshaller,
                              include_va=False,
                              source_location=None):
    '''Generate an alias between @marshaller and @real_marshaller, including
    an optional alias for va_list marshallers'''
    if source_location:
        outfile.write('/* {} */\n'.format(source_location))

    outfile.write('#define {}\t{}\n'.format(marshaller, real_marshaller))

    if include_va:
        outfile.write('#define {}v\t{}v\n'.format(marshaller, real_marshaller))

    outfile.write('\n')


def generate_marshallers_header(outfile, retval, params,
                                prefix='g_cclosure_user_marshal',
                                internal=False,
                                include_va=False, source_location=None):
    '''Generate a declaration for a marshaller function, to be used in the header,
    with the given @retval, @params, and @prefix. An optional va_list marshaller
    for the same arguments is also generated. The generated buffer is written to
    the @outfile stream object.'''
    if source_location:
        outfile.write('/* {} */\n'.format(source_location))

    if internal:
        visibility = Visibility.INTERNAL
    else:
        visibility = Visibility.EXTERN

    signature = generate_prototype(retval, params, prefix, visibility, False)
    if include_va:
        signature += generate_prototype(retval, params, prefix, visibility, True)
    signature += ['']

    outfile.write('\n'.join(signature))
    outfile.write('\n')


def generate_marshallers_body(outfile, retval, params,
                              prefix='g_cclosure_user_marshal',
                              include_prototype=True,
                              internal=False,
                              include_va=False, source_location=None):
    '''Generate a definition for a marshaller function, to be used in the source,
    with the given @retval, @params, and @prefix. An optional va_list marshaller
    for the same arguments is also generated. The generated buffer is written to
    the @outfile stream object.'''
    if source_location:
        outfile.write('/* {} */\n'.format(source_location))

    if include_prototype:
        # Declaration visibility
        if internal:
            decl_visibility = Visibility.INTERNAL
        else:
            decl_visibility = Visibility.EXTERN
        proto = ['/* Prototype for -Wmissing-prototypes */']
        # Add C++ guards in case somebody compiles the generated code
        # with a C++ compiler
        proto += ['G_BEGIN_DECLS']
        proto += generate_prototype(retval, params, prefix, decl_visibility, False)
        proto += ['G_END_DECLS']
        outfile.write('\n'.join(proto))
        outfile.write('\n')

    body = generate_body(retval, params, prefix, False)
    outfile.write('\n'.join(body))
    outfile.write('\n\n')

    if include_va:
        if include_prototype:
            # Declaration visibility
            if internal:
                decl_visibility = Visibility.INTERNAL
            else:
                decl_visibility = Visibility.EXTERN
            proto = ['/* Prototype for -Wmissing-prototypes */']
            # Add C++ guards here as well
            proto += ['G_BEGIN_DECLS']
            proto += generate_prototype(retval, params, prefix, decl_visibility, True)
            proto += ['G_END_DECLS']
            outfile.write('\n'.join(proto))
            outfile.write('\n')

        body = generate_body(retval, params, prefix, True)
        outfile.write('\n'.join(body))
        outfile.write('\n\n')


def parse_args():
    arg_parser = argparse.ArgumentParser(description='Generate signal marshallers for GObject')
    arg_parser.add_argument('--prefix', metavar='STRING',
                            default='g_cclosure_user_marshal',
                            help='Specify marshaller prefix')
    arg_parser.add_argument('--output', metavar='FILE',
                            type=argparse.FileType('w'),
                            default=sys.stdout,
                            help='Write output into the specified file')
    arg_parser.add_argument('--skip-source',
                            action='store_true',
                            help='Skip source location comments')
    arg_parser.add_argument('--internal',
                            action='store_true',
                            help='Mark generated functions as internal')
    arg_parser.add_argument('--valist-marshallers',
                            action='store_true',
                            help='Generate va_list marshallers')
    arg_parser.add_argument('-v', '--version',
                            action='store_true',
                            dest='show_version',
                            help='Print version information, and exit')
    arg_parser.add_argument('--g-fatal-warnings',
                            action='store_true',
                            dest='fatal_warnings',
                            help='Make warnings fatal')
    arg_parser.add_argument('--include-header', metavar='HEADER', nargs='?',
                            action='append',
                            dest='include_headers',
                            help='Include the specified header in the body')
    arg_parser.add_argument('--pragma-once',
                            action='store_true',
                            help='Use "pragma once" as the inclusion guard')
    arg_parser.add_argument('-D',
                            action='append',
                            dest='cpp_defines',
                            default=[],
                            help='Pre-processor define')
    arg_parser.add_argument('-U',
                            action='append',
                            dest='cpp_undefines',
                            default=[],
                            help='Pre-processor undefine')
    arg_parser.add_argument('files', metavar='FILE', nargs='*',
                            type=argparse.FileType('r'),
                            help='Files with lists of marshallers to generate, ' +
                            'or "-" for standard input')
    arg_parser.add_argument('--prototypes',
                            action='store_true',
                            help='Generate the marshallers prototype in the C code')
    arg_parser.add_argument('--header',
                            action='store_true',
                            help='Generate C headers')
    arg_parser.add_argument('--body',
                            action='store_true',
                            help='Generate C code')

    group = arg_parser.add_mutually_exclusive_group()
    group.add_argument('--stdinc',
                       action='store_true',
                       dest='stdinc', default=True,
                       help='Include standard marshallers')
    group.add_argument('--nostdinc',
                       action='store_false',
                       dest='stdinc', default=True,
                       help='Use standard marshallers')

    group = arg_parser.add_mutually_exclusive_group()
    group.add_argument('--quiet',
                       action='store_true',
                       help='Only print warnings and errors')
    group.add_argument('--verbose',
                       action='store_true',
                       help='Be verbose, and include debugging information')

    args = arg_parser.parse_args()

    if args.show_version:
        print(VERSION_STR)
        sys.exit(0)

    return args


def generate(args):
    # Backward compatibility hack; some projects use both arguments to
    # generate the marshallers prototype in the C source, even though
    # it's not really a supported use case. We keep this behaviour by
    # forcing the --prototypes and --body arguments instead. We make this
    # warning non-fatal even with --g-fatal-warnings, as it's a deprecation
    compatibility_mode = False
    if args.header and args.body:
        print_warning('Using --header and --body at the same time is deprecated; ' +
                      'use --body --prototypes instead', False)
        args.prototypes = True
        args.header = False
        compatibility_mode = True

    if args.header:
        generate_header_preamble(args.output,
                                 prefix=args.prefix,
                                 std_includes=args.stdinc,
                                 use_pragma=args.pragma_once)
    elif args.body:
        generate_body_preamble(args.output,
                               std_includes=args.stdinc,
                               include_headers=args.include_headers,
                               cpp_defines=args.cpp_defines,
                               cpp_undefines=args.cpp_undefines)

    seen_marshallers = set()

    for infile in args.files:
        if not args.quiet:
            print_info('Reading {}...'.format(infile.name))

        line_count = 0
        for line in infile:
            line_count += 1

            if line == '\n' or line.startswith('#'):
                continue

            matches = re.match(r'^([A-Z0-9]+)\s?:\s?([A-Z0-9,\s]+)$', line.strip())
            if not matches or len(matches.groups()) != 2:
                print_warning('Invalid entry: "{}"'.format(line.strip()), args.fatal_warnings)
                continue

            if not args.skip_source:
                location = '{} ({}:{:d})'.format(line.strip(), infile.name, line_count)
            else:
                location = None

            retval = matches.group(1).strip()
            params = [x.strip() for x in matches.group(2).split(',')]
            check_args(retval, params, args.fatal_warnings)

            raw_marshaller = generate_marshaller_name(args.prefix, retval, params, False)
            if raw_marshaller in seen_marshallers:
                if args.verbose:
                    print_info('Skipping repeated marshaller {}'.format(line.strip()))
                continue

            if args.header:
                if args.verbose:
                    print_info('Generating declaration for {}'.format(line.strip()))
                generate_std_alias = False
                if args.stdinc:
                    std_marshaller = generate_marshaller_name(STD_PREFIX, retval, params)
                    if std_marshaller in GOBJECT_MARSHALLERS:
                        if args.verbose:
                            print_info('Skipping default marshaller {}'.format(line.strip()))
                        generate_std_alias = True

                marshaller = generate_marshaller_name(args.prefix, retval, params)
                if generate_std_alias:
                    generate_marshaller_alias(args.output, marshaller, std_marshaller,
                                              source_location=location,
                                              include_va=args.valist_marshallers)
                else:
                    generate_marshallers_header(args.output, retval, params,
                                                prefix=args.prefix,
                                                internal=args.internal,
                                                include_va=args.valist_marshallers,
                                                source_location=location)
                # If the marshaller is defined using a deprecated token, we want to maintain
                # compatibility and generate an alias for the old name pointing to the new
                # one
                if marshaller != raw_marshaller:
                    if args.verbose:
                        print_info('Generating alias for deprecated tokens')
                    generate_marshaller_alias(args.output, raw_marshaller, marshaller,
                                              include_va=args.valist_marshallers)
            elif args.body:
                if args.verbose:
                    print_info('Generating definition for {}'.format(line.strip()))
                generate_std_alias = False
                if args.stdinc:
                    std_marshaller = generate_marshaller_name(STD_PREFIX, retval, params)
                    if std_marshaller in GOBJECT_MARSHALLERS:
                        if args.verbose:
                            print_info('Skipping default marshaller {}'.format(line.strip()))
                        generate_std_alias = True
                marshaller = generate_marshaller_name(args.prefix, retval, params)
                if generate_std_alias:
                    # We need to generate the alias if we are in compatibility mode
                    if compatibility_mode:
                        generate_marshaller_alias(args.output, marshaller, std_marshaller,
                                                  source_location=location,
                                                  include_va=args.valist_marshallers)
                else:
                    generate_marshallers_body(args.output, retval, params,
                                              prefix=args.prefix,
                                              internal=args.internal,
                                              include_prototype=args.prototypes,
                                              include_va=args.valist_marshallers,
                                              source_location=location)
                if compatibility_mode and marshaller != raw_marshaller:
                    if args.verbose:
                        print_info('Generating alias for deprecated tokens')
                    generate_marshaller_alias(args.output, raw_marshaller, marshaller,
                                              include_va=args.valist_marshallers)

            seen_marshallers.add(raw_marshaller)

        if args.header:
            generate_header_postamble(args.output, prefix=args.prefix, use_pragma=args.pragma_once)


if __name__ == '__main__':
    args = parse_args()

    with args.output:
        generate(args)
