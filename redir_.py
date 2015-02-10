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

print "adding redirections:"
if options.portnum == 5554:
    child.sendline('redir add tcp:11108:10000')
elif options.portnum == 5556:
    child.sendline('redir add tcp:11112:10000')
elif options.portnum == 5558:
    child.sendline('redir add tcp:11116:10000')
elif options.portnum == 5560:
    child.sendline('redir add tcp:11120:10000')
elif options.portnum == 5562:
    child.sendline('redir add tcp:11124:10000')

r = child.expect(["OK", "KO: host port already active", "KO:.*"])
if r == 2:
    raise Exception(child.after)


child.sendline("redir list")
child.expect("OK")
print child.before
