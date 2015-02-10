import pexpect
from optparse import OptionParser


parser = OptionParser("usage: %prog [options] arg arg")
parser.add_option("-H", "--host", dest="hostname", type="string",
                          help="specify hostname to run on")
parser.add_option("-p", "--port", dest="portnum",
                          type="int", help="port number to run on")
   
(options, args) = parser.parse_args()

print (len(args))

#if len(args) != 2:
#        parser.error("incorrect number of arguments")
#        hostname = options.hostname
#        portnum = options.portnum

print options.hostname
cmd = 'telnet %s %d' % (options.hostname,options.portnum)

child = pexpect.spawn(cmd)
child.expect('OK')


print "deleting redirections:"
child.sendline('redir del tcp:11108')
child.sendline('redir del tcp:11112')
child.sendline('redir del tcp:11116')
child.sendline('redir del tcp:11120')
child.sendline('redir del tcp:11124')


child.sendline("redir list")
child.expect("OK")
print child.before
