#!/bin/sh
# Test redirecting from CGI
# $Id$
echo "Status: 302 Moved"
echo "Location: http://${HTTP_HOST}/"
echo
