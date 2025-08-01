
# GLib Testing Framework Utility			-*- Mode: python; -*-
# Copyright (C) 2007 Imendio AB
# Authors: Tim Janik
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

# Deprecated: Since GLib 2.62, gtester and gtester-report have been deprecated
# in favour of TAP.

import datetime
import optparse
import sys, re, xml.dom.minidom

try:
    import subunit
    from subunit import iso8601
    from testtools.content import Content, ContentType
    mime_utf8 = ContentType('text', 'plain', {'charset': 'utf8'})
except ImportError:
    subunit = None


# xml utilities
def find_child (node, child_name):
  for child in node.childNodes:
    if child.nodeName == child_name:
      return child
  return None
def list_children (node, child_name):
  rlist = []
  for child in node.childNodes:
    if child.nodeName == child_name:
      rlist += [ child ]
  return rlist
def find_node (node, name = None):
  if not node or node.nodeName == name or not name:
    return node
  for child in node.childNodes:
    c = find_node (child, name)
    if c:
      return c
  return None
def node_as_text (node, name = None):
  if name:
    node = find_node (node, name)
  txt = ''
  if node:
    if node.nodeValue:
      txt += node.nodeValue
    for child in node.childNodes:
      txt += node_as_text (child)
  return txt
def attribute_as_text (node, aname, node_name = None):
  node = find_node (node, node_name)
  if not node:
    return ''
  attr = node.attributes.get (aname, '')
  if hasattr (attr, 'value'):
    return attr.value
  return ''

# HTML utilities
def html_indent_string (n):
  uncollapsible_space = ' &nbsp;' # HTML won't compress alternating sequences of ' ' and '&nbsp;'
  string = ''
  for i in range (0, int((n + 1) / 2)):
    string += uncollapsible_space
  return string

# TestBinary object, instantiated per test binary in the log file
class TestBinary:
  def __init__ (self, name):
    self.name = name
    self.testcases = []
    self.duration = 0
    self.success_cases = 0
    self.skipped_cases = 0
    self.file = '???'
    self.random_seed = ''

# base class to handle processing/traversion of XML nodes
class TreeProcess:
  def __init__ (self):
    self.nest_level = 0
  def trampoline (self, node):
    name = node.nodeName
    if name == '#text':
      self.handle_text (node)
    else:
      try:	method = getattr (self, 'handle_' + re.sub ('[^a-zA-Z0-9]', '_', name))
      except:	method = None
      if method:
        return method (node)
      else:
        return self.process_recursive (name, node)
  def process_recursive (self, node_name, node):
    self.process_children (node)
  def process_children (self, node):
    self.nest_level += 1
    for child in node.childNodes:
      self.trampoline (child)
    self.nest_level += 1

# test report reader, this class collects some statistics and merges duplicate test binary runs
class ReportReader (TreeProcess):
  def __init__ (self):
    TreeProcess.__init__ (self)
    self.binary_names = []
    self.binaries = {}
    self.last_binary = None
    self.info = {}
  def binary_list (self):
    lst = []
    for name in self.binary_names:
      lst += [ self.binaries[name] ]
    return lst
  def get_info (self):
    return self.info
  def handle_info (self, node):
    dn = find_child (node, 'package')
    self.info['package'] = node_as_text (dn)
    dn = find_child (node, 'version')
    self.info['version'] = node_as_text (dn)
    dn = find_child (node, 'revision')
    if dn is not None:
        self.info['revision'] = node_as_text (dn)
  def handle_testcase (self, node):
    self.last_binary.testcases += [ node ]
    result = attribute_as_text (node, 'result', 'status')
    if result == 'success':
      self.last_binary.success_cases += 1
    if bool (int (attribute_as_text (node, 'skipped') + '0')):
      self.last_binary.skipped_cases += 1
  def handle_text (self, node):
    pass
  def handle_testbinary (self, node):
    path = node.attributes.get ('path', None).value
    if self.binaries.get (path, -1) == -1:
      self.binaries[path] = TestBinary (path)
      self.binary_names += [ path ]
    self.last_binary = self.binaries[path]
    dn = find_child (node, 'duration')
    dur = node_as_text (dn)
    try:        dur = float (dur)
    except:     dur = 0
    if dur:
      self.last_binary.duration += dur
    bin = find_child (node, 'binary')
    if bin:
      self.last_binary.file = attribute_as_text (bin, 'file')
    rseed = find_child (node, 'random-seed')
    if rseed:
      self.last_binary.random_seed = node_as_text (rseed)
    self.process_children (node)


class ReportWriter(object):
    """Base class for reporting."""

    def __init__(self, binary_list):
        self.binaries = binary_list

    def _error_text(self, node):
        """Get a string representing the error children of node."""
        rlist = list_children(node, 'error')
        txt = ''
        for enode in rlist:
            txt += node_as_text (enode)
            if txt and txt[-1] != '\n':
                txt += '\n'
        return txt


class HTMLReportWriter(ReportWriter):
  # Javascript/CSS snippet to toggle element visibility
  cssjs = r'''
  <style type="text/css" media="screen">
    .VisibleSection { }
    .HiddenSection  { display: none; }
  </style>
  <script language="javascript" type="text/javascript"><!--
  function toggle_display (parentid, tagtype, idmatch, keymatch) {
    ptag = document.getElementById (parentid);
    tags = ptag.getElementsByTagName (tagtype);
    for (var i = 0; i < tags.length; i++) {
      tag = tags[i];
      var key = tag.getAttribute ("keywords");
      if (tag.id.indexOf (idmatch) == 0 && key && key.match (keymatch)) {
        if (tag.className.indexOf ("HiddenSection") >= 0)
          tag.className = "VisibleSection";
        else
          tag.className = "HiddenSection";
      }
    }
  }
  message_array = Array();
  function view_testlog (wname, file, random_seed, tcase, msgtitle, msgid) {
      txt = message_array[msgid];
      var w = window.open ("", // URI
                           wname,
                           "resizable,scrollbars,status,width=790,height=400");
      var doc = w.document;
      doc.write ("<h2>File: " + file + "</h2>\n");
      doc.write ("<h3>Case: " + tcase + "</h3>\n");
      doc.write ("<strong>Random Seed:</strong> <code>" + random_seed + "</code> <br /><br />\n");
      doc.write ("<strong>" + msgtitle + "</strong><br />\n");
      doc.write ("<pre>");
      doc.write (txt);
      doc.write ("</pre>\n");
      doc.write ("<a href=\'javascript:window.close()\'>Close Window</a>\n");
      doc.close();
  }
  --></script>
  '''
  def __init__ (self, info, binary_list):
    ReportWriter.__init__(self, binary_list)
    self.info = info
    self.bcounter = 0
    self.tcounter = 0
    self.total_tcounter = 0
    self.total_fcounter = 0
    self.total_duration = 0
    self.indent_depth = 0
    self.lastchar = ''
  def oprint (self, message):
    sys.stdout.write (message)
    if message:
      self.lastchar = message[-1]
  def handle_info (self):
    if 'package' in self.info and 'version' in self.info:
      self.oprint ('<h3>Package: %(package)s, version: %(version)s</h3>\n' % self.info)
      if 'revision' in self.info:
          self.oprint ('<h5>Report generated from: %(revision)s</h5>\n' % self.info)
  def handle_text (self, node):
    self.oprint (node.nodeValue)
  def handle_testcase (self, node, binary):
    skipped = bool (int (attribute_as_text (node, 'skipped') + '0'))
    if skipped:
      return            # skipped tests are uninteresting for HTML reports
    path = attribute_as_text (node, 'path')
    duration = node_as_text (node, 'duration')
    result = attribute_as_text (node, 'result', 'status')
    rcolor = {
      'success': 'bgcolor="lightgreen"',
      'failed':  'bgcolor="red"',
    }.get (result, '')
    if result != 'success':
      duration = '-'    # ignore bogus durations
    self.oprint ('<tr id="b%u_t%u_" keywords="%s all" class="HiddenSection">\n' % (self.bcounter, self.tcounter, result))
    self.oprint ('<td>%s %s</td> <td align="right">%s</td> \n' % (html_indent_string (4), path, duration))
    perflist = list_children (node, 'performance')
    if result != 'success':
      txt = self._error_text(node)
      txt = re.sub (r'"', r'\\"', txt)
      txt = re.sub (r'\n', r'\\n', txt)
      txt = re.sub (r'&', r'&amp;', txt)
      txt = re.sub (r'<', r'&lt;', txt)
      self.oprint ('<script language="javascript" type="text/javascript">message_array["b%u_t%u_"] = "%s";</script>\n' % (self.bcounter, self.tcounter, txt))
      self.oprint ('<td align="center"><a href="javascript:view_testlog (\'%s\', \'%s\', \'%s\', \'%s\', \'Output:\', \'b%u_t%u_\')">Details</a></td>\n' %
                   ('TestResultWindow', binary.file, binary.random_seed, path, self.bcounter, self.tcounter))
    elif perflist:
      presults = []
      for perf in perflist:
        pmin = bool (int (attribute_as_text (perf, 'minimize')))
        pmax = bool (int (attribute_as_text (perf, 'maximize')))
        pval = float (attribute_as_text (perf, 'value'))
        txt = node_as_text (perf)
        txt = re.sub (r'&', r'&amp;', txt)
        txt = re.sub (r'<', r'&gt;', txt)
        txt = '<strong>Performance(' + (pmin and '<em>minimized</em>' or '<em>maximized</em>') + '):</strong> ' + txt.strip() + '<br />\n'
        txt = re.sub (r'"', r'\\"', txt)
        txt = re.sub (r'\n', r'\\n', txt)
        presults += [ (pval, txt) ]
      presults.sort()
      ptxt = ''.join ([e[1] for e in presults])
      self.oprint ('<script language="javascript" type="text/javascript">message_array["b%u_t%u_"] = "%s";</script>\n' % (self.bcounter, self.tcounter, ptxt))
      self.oprint ('<td align="center"><a href="javascript:view_testlog (\'%s\', \'%s\', \'%s\', \'%s\', \'Test Results:\', \'b%u_t%u_\')">Details</a></td>\n' %
                   ('TestResultWindow', binary.file, binary.random_seed, path, self.bcounter, self.tcounter))
    else:
      self.oprint ('<td align="center">-</td>\n')
    self.oprint ('<td align="right" %s>%s</td>\n' % (rcolor, result))
    self.oprint ('</tr>\n')
    self.tcounter += 1
    self.total_tcounter += 1
    self.total_fcounter += result != 'success'
  def handle_binary (self, binary):
    self.tcounter = 1
    self.bcounter += 1
    self.total_duration += binary.duration
    self.oprint ('<tr><td><strong>%s</strong></td><td align="right">%f</td> <td align="center">\n' % (binary.name, binary.duration))
    erlink, oklink = ('', '')
    real_cases = len (binary.testcases) - binary.skipped_cases
    if binary.success_cases < real_cases:
      erlink = 'href="javascript:toggle_display (\'ResultTable\', \'tr\', \'b%u_\', \'failed\')"' % self.bcounter
    if binary.success_cases:
      oklink = 'href="javascript:toggle_display (\'ResultTable\', \'tr\', \'b%u_\', \'success\')"' % self.bcounter
    if real_cases != 0:
        self.oprint ('<a %s>ER</a>\n' % erlink)
        self.oprint ('<a %s>OK</a>\n' % oklink)
        self.oprint ('</td>\n')
        perc = binary.success_cases * 100.0 / real_cases
        pcolor = {
          100 : 'bgcolor="lightgreen"',
          0   : 'bgcolor="red"',
        }.get (int (perc), 'bgcolor="yellow"')
        self.oprint ('<td align="right" %s>%.2f%%</td>\n' % (pcolor, perc))
        self.oprint ('</tr>\n')
    else:
        self.oprint ('Empty\n')
        self.oprint ('</td>\n')
        self.oprint ('</tr>\n')
    for tc in binary.testcases:
      self.handle_testcase (tc, binary)
  def handle_totals (self):
    self.oprint ('<tr>')
    self.oprint ('<td><strong>Totals:</strong> %u Binaries, %u Tests, %u Failed, %u Succeeded</td>' %
                 (self.bcounter, self.total_tcounter, self.total_fcounter, self.total_tcounter - self.total_fcounter))
    self.oprint ('<td align="right">%f</td>\n' % self.total_duration)
    self.oprint ('<td align="center">-</td>\n')
    if self.total_tcounter != 0:
        perc = (self.total_tcounter - self.total_fcounter) * 100.0 / self.total_tcounter
    else:
        perc = 0.0
    pcolor = {
      100 : 'bgcolor="lightgreen"',
      0   : 'bgcolor="red"',
    }.get (int (perc), 'bgcolor="yellow"')
    self.oprint ('<td align="right" %s>%.2f%%</td>\n' % (pcolor, perc))
    self.oprint ('</tr>\n')
  def printout (self):
    self.oprint ('<html><head>\n')
    self.oprint ('<title>GTester Unit Test Report</title>\n')
    self.oprint (self.cssjs)
    self.oprint ('</head>\n')
    self.oprint ('<body>\n')
    self.oprint ('<h2>GTester Unit Test Report</h2>\n')
    self.handle_info ()
    self.oprint ('<p style="color:red;font-weight:bold"><blink>'
                 'Deprecated: Since GLib 2.62, gtester and gtester-report are '
                 'deprecated. Port to TAP.</blink></p>\n');
    self.oprint ('<table id="ResultTable" width="100%" border="1">\n<tr>\n')
    self.oprint ('<th>Program / Testcase </th>\n')
    self.oprint ('<th style="width:8em">Duration (sec)</th>\n')
    self.oprint ('<th style="width:5em">View</th>\n')
    self.oprint ('<th style="width:5em">Result</th>\n')
    self.oprint ('</tr>\n')
    for tb in self.binaries:
      self.handle_binary (tb)
    self.handle_totals()
    self.oprint ('</table>\n')
    self.oprint ('</body>\n')
    self.oprint ('</html>\n')


class SubunitWriter(ReportWriter):
    """Reporter to output a subunit stream."""

    def printout(self):
        reporter = subunit.TestProtocolClient(sys.stdout)
        for binary in self.binaries:
            for tc in binary.testcases:
                test = GTestCase(tc, binary)
                test.run(reporter)


class GTestCase(object):
    """A representation of a gtester test result as a pyunit TestCase."""

    def __init__(self, case, binary):
        """Create a GTestCase for case 'case' from binary program 'binary'."""
        self._case = case
        self._binary = binary
        # the name of the case - e.g. /dbusmenu/glib/objects/menuitem/props_boolstr
        self._path = attribute_as_text(self._case, 'path')

    def id(self):
        """What test is this? Returns the gtester path for the testcase."""
        return self._path

    def _get_details(self):
        """Calculate a details dict for the test - attachments etc."""
        details = {}
        result = attribute_as_text(self._case, 'result', 'status')
        details['filename'] = Content(mime_utf8, lambda:[self._binary.file])
        details['random_seed'] = Content(mime_utf8,
            lambda:[self._binary.random_seed])
        if self._get_outcome() == 'addFailure':
            # Extract the error details. Skips have no details because its not
            # skip like unittest does, instead the runner just bypasses N test.
            txt = self._error_text(self._case)
            details['error'] = Content(mime_utf8, lambda:[txt])
        if self._get_outcome() == 'addSuccess':
            # Successful tests may have performance metrics.
            perflist = list_children(self._case, 'performance')
            if perflist:
                presults = []
                for perf in perflist:
                    pmin = bool (int (attribute_as_text (perf, 'minimize')))
                    pmax = bool (int (attribute_as_text (perf, 'maximize')))
                    pval = float (attribute_as_text (perf, 'value'))
                    txt = node_as_text (perf)
                    txt = 'Performance(' + (pmin and 'minimized' or 'maximized'
                        ) + '): ' + txt.strip() + '\n'
                    presults += [(pval, txt)]
                presults.sort()
                perf_details = [e[1] for e in presults]
                details['performance'] = Content(mime_utf8, lambda:perf_details)
        return details

    def _get_outcome(self):
        if int(attribute_as_text(self._case, 'skipped') + '0'):
            return 'addSkip'
        outcome = attribute_as_text(self._case, 'result', 'status')
        if outcome == 'success':
            return 'addSuccess'
        else:
            return 'addFailure'

    def run(self, result):
        time = datetime.datetime.utcnow().replace(tzinfo=iso8601.Utc())
        result.time(time)
        result.startTest(self)
        try:
            outcome = self._get_outcome()
            details = self._get_details()
            # Only provide a duration IFF outcome == 'addSuccess' - the main
            # parser claims bogus results otherwise: in that case emit time as
            # zero perhaps.
            if outcome == 'addSuccess':
                duration = float(node_as_text(self._case, 'duration'))
                duration = duration * 1000000
                timedelta = datetime.timedelta(0, 0, duration)
                time = time + timedelta
                result.time(time)
            getattr(result, outcome)(self, details=details)
        finally:
            result.stopTest(self)



# main program handling
def parse_opts():
    """Parse program options.

    :return: An options object and the program arguments.
    """
    parser = optparse.OptionParser()
    parser.version = '2.84.2'
    parser.usage = "%prog [OPTIONS] <gtester-log.xml>"
    parser.description = "Generate HTML reports from the XML log files generated by gtester."
    parser.epilog = "gtester-report (GLib utils) version %s."% (parser.version,)
    parser.add_option("-v", "--version", action="store_true", dest="version", default=False,
        help="Show program version.")
    parser.add_option("-s", "--subunit", action="store_true", dest="subunit", default=False,
        help="Output subunit [See https://launchpad.net/subunit/"
            " Needs python-subunit]")
    options, files = parser.parse_args()
    if options.version:
        print(parser.epilog)
        return None, None
    if len(files) != 1:
        parser.error("Must supply a log file to parse.")
    if options.subunit and subunit is None:
        parser.error("python-subunit is not installed.")
    return options, files


def main():
  options, files = parse_opts()
  if options is None:
    return 0

  print("Deprecated: Since GLib 2.62, gtester and gtester-report are "
        "deprecated. Port to TAP.", file=sys.stderr)

  xd = xml.dom.minidom.parse (files[0])
  rr = ReportReader()
  rr.trampoline (xd)
  if not options.subunit:
      HTMLReportWriter(rr.get_info(), rr.binary_list()).printout()
  else:
      SubunitWriter(rr.get_info(), rr.binary_list()).printout()


if __name__ == '__main__':
  main()
