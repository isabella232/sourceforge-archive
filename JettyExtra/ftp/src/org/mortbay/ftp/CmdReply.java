// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.ftp;


// ===========================================================================
/** FTP Command Reply Stream
 * <p> 
 * <p><h4>Notes</h4>
 * <p> notes...
 *
 * <p><h4>Usage</h4>
 * <pre>
 * example();
 * </pre>
 *
 * @see 
 * @version $Id$
 * @author Greg Wilkins
*/
class CmdReply
{
    /* ------------------------------------------------------------ */
    /* First code digit */
    public final static char
        d0PositivePreliminary    = '1',
        d0PositiveCompletion     = '2',
        d0PositiveIntermediate   = '3',
        d0NegativeTransient      = '4',
        d0NegativePermanent      = '5';
    
    /* ------------------------------------------------------------ */
    /* Second code digit */
    public final static char
        d1Syntax           = '0',
        d1Information      = '1',
        d1Connections      = '2',
        d1Authentication   = '3',
        d1Unspecified      = '4',
        d1FileSystem       = '5';

    /* ------------------------------------------------------------ */
    /* Codes */
    public final static String
        codeRestartMarker                 = "110",
        codeServiceNotReady               = "120",
        codeDataAlreadyOpen               = "125", 
        codeFileStatusOK                  = "150", 
        codeCommandOK                     = "200", 
        codeCommandNotRequired            = "202", 
        codeSystemStatus                  = "211", 
        codeDirectoryStatus               = "212", 
        codeFileStatus                    = "213", 
        codeHelpMessage                   = "214", 
        codeSystemType                    = "215", 
        codeServiceReady                  = "220", 
        codeServiceClosing                = "221", 
        codeDataOpen                      = "225", 
        codeClosingData                   = "226", 
        codeEnteringPassiveMode           = "227", 
        codeUserLoggedIn                  = "230", 
        codeRequestedFileActionOK         = "250", 
        codePathCreated                   = "257", 
        codeUsernameNeedsPassword         = "331", 
        codeNeedAccount                   = "332", 
        codeRequestedFileActionPending    = "350", 
        codeServiceNotAvailable           = "421", 
        codeDataConnectionProblem         = "425", 
        codeTransferAborted               = "426", 
        codeFileUnavailableTransient      = "450", 
        codeLocalError                    = "451", 
        codeInsufficientSpace             = "452", 
        codeSyntaxErrorCommand            = "500", 
        codeSyntaxErrorParameter          = "501", 
        codeCommandNotImplemented         = "502", 
        codeBadCommandSequence            = "503", 
        codeCommandNotImplementedParameter= "504", 
        codeNotLoggedIn                   = "530", 
        codeNeedAccountForRequest         = "532", 
        codeFileUnavailablePermanent      = "550", 
        codePageTypeUnknown               = "551", 
        codeExceededStorageAllocation     = "552", 
        codeFileNameNotAllowed            = "553";
    
                                          
    /* ------------------------------------------------------------ */
    public String code;                   
    public String text;                   

    /* ------------------------------------------------------------ */
    boolean preliminary()
    {
        switch (code.charAt(0))
        {
          case d0PositivePreliminary:
              return true;
          default:
              return false;
        }
    }

    /* ------------------------------------------------------------ */
    boolean intermediate()
    {
        switch (code.charAt(0))
        {
          case d0PositiveIntermediate:
              return true;
          default:
              return false;
        }
    }

    
    /* ------------------------------------------------------------ */
    boolean positive()
    {
        switch (code.charAt(0))
        {
          case d0PositivePreliminary:
          case d0PositiveCompletion:
          case d0PositiveIntermediate:
              return true;
              
          case d0NegativeTransient:
          case d0NegativePermanent:
          default:
              return false;
        }
    }
    
    /* ------------------------------------------------------------ */
    boolean transferComplete()
    {
        switch (code.charAt(0))
        {
          case d0PositiveCompletion:
          case d0NegativePermanent:
              return true;
              
          case d0PositiveIntermediate:
          case d0PositivePreliminary:
          case d0NegativeTransient:
          default:
              return false;
        }
    }

    
    /* ------------------------------------------------------------ */
    /** Check type of second code digit
     * @param type type digit d1XXXX
     * @return true if code is of type
     */
    public boolean isType(char type)
    {
        return (code.length()==3 && code.charAt(1)==type);
    }
    
        
    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (text.indexOf('\n')>=0)
            return "[Code="+code+",Text=\n"+text+"\n]";
        return "[Code="+code+",Text="+text+"]";
    }

}
