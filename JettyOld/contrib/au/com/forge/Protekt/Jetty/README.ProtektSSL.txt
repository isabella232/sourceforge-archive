Introduction
============
This file contains information about the ProtektListener class that is
included in the Jetty distribution.


Who is FORGE Information Technology?
------------------------------------
FORGE Information Technology is a mature software development company
specialising in telecommunications and cross-platform distributed solutions.
With its leading edge R&D, the company has a successful history building
and deploying complex commercial systems in both domestic and overseas
companies.

Contact information:
--------------------
FORGE Information Technology Pty. Ltd.
Suite 116, Bay 9
The Locomotive Workshop
Australian Technology Park
Eveleigh,
NSW, 1430
Australia

Postal:

PO Box 598
Alexandria
NSW, 1435
Australia

Phone:	+61 2 9209 4152
Fax:	+61 2 9209 4172

WWW:	http://www.forge.com.au
email:	info@forge.com.au


What is Protekt Encryption 3.0?
-------------------------------
Protekt Encryption 3.0 is an all Java implementation of the TLS 1.0 and
SSL 3.0 protocols. Protekt Encryption 3.0 is compatible with Java2 and uses the
Java Cryptography Extensions and Java Security APIs to provide vendor
independent operation.

Protekt Encryption 3.0 is not an open source or free product. It is a
commercial library for software developers wishing to add SSL functionality
to their software.

WWW:	http://www.protekt.com
email:	protekt@forge.com.au


What is the ProtektListener?
----------------------------
The ProtektListener class adds TLS/SSL support to Jetty using the Protekt
Encryption 3.0 library.

Recent changes to the Jetty architecture have made it very easy to
use custom sockets for accepting connections. By extending the
HttpListener class, the ProtektListener class only needs to override two
methods to add TLS/SSL support to Jetty.

The HttpListener class method implementations are used as is, and the
newServerSocket and accept methods from the ThreadedServer class are
re-implemented.

The newServerSocket method creates au.com.forge.Protekt.SSLServerSockets
instead of standard java.net.ServerSockets.

The accept method runs the TLS/SSL handshake before returning the accepted
connection, allowing the HttpListener methods to work with the returned
socket as if it were a standard java.net.Socket.

FORGE Information Technology has donated the ProtektListener class to
the Jetty project.


How to use the  ProtektListener?
--------------------------------
You must have a distribution of Protekt installed.
The ProtektListener class must be configured as the HttpListener for
the servers that you wish to talk https.  A simple configuration
file for TLS/SSL is:


SERVERS                                      : main
main.CLASS				     : com.mortbay.HTTP.HttpServer
main.STACKS				     : root
main.LISTENER.all.CLASS                      : au.com.forge.security.tls.jetty.ProtektListener
main.LISTENER.all.ADDRS                      : 0.0.0.0:8080
main.root.PATHS			             : /
main.root.HANDLERS		             : Param;Servlet;NotFound
main.root.EXCEPTIONS		             : Default
main.root.Param.CLASS			     : com.mortbay.HTTP.Handler.ParamHandler
main.root.Servlet.CLASS		             : com.mortbay.HTTP.Handler.ServletHandler
main.root.Servlet.PROPERTY.SERVLET.Dump.CLASS: com.mortbay.Servlets.Dump
main.root.Servlet.PROPERTY.SERVLET.Dump.PATHS: /
main.root.NotFound.CLASS		     : com.mortbay.HTTP.Handler.NotFoundHandler
main.root.Default.CLASS			     : com.mortbay.HTTP.Handler.DefaultExceptionHandler






