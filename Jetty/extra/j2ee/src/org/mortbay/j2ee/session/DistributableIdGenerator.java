// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.mortbay.j2ee.session;

public class
  DistributableIdGenerator
  extends GUIDGenerator
{
  public synchronized Object
    clone()
    {
      DistributableIdGenerator dig=(DistributableIdGenerator)super.clone();
      return dig;
    }
}
