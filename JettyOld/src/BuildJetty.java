class BuildJetty { Class[] classes = {
    com.mortbay.Base.Code.class,
    com.mortbay.Base.CodeException.class,
    com.mortbay.Base.DateCache.class,
    com.mortbay.Base.FileLogSink.class,
    com.mortbay.Base.Frame.class,
    com.mortbay.Base.Log.class,
    com.mortbay.Base.LogSink.class,
    com.mortbay.Base.RolloverFileLogSink.class,
    com.mortbay.Base.Test.class,
    com.mortbay.Base.TestHarness.class,
    com.mortbay.FTP.DataPort.class,
    com.mortbay.FTP.Ftp.class,
    com.mortbay.FTP.FtpCmdStreamException.class,
    com.mortbay.FTP.FtpException.class,
    com.mortbay.FTP.FtpReplyException.class,
    com.mortbay.FTP.TestFtp.class,
    com.mortbay.HTML.Applet.class,
    com.mortbay.HTML.Block.class,
    com.mortbay.HTML.Break.class,
    com.mortbay.HTML.Comment.class,
    com.mortbay.HTML.Composite.class,
    com.mortbay.HTML.DefList.class,
    com.mortbay.HTML.Element.class,
    com.mortbay.HTML.Font.class,
    com.mortbay.HTML.Form.class,
    com.mortbay.HTML.Frame.class,
    com.mortbay.HTML.FrameSet.class,
    com.mortbay.HTML.Heading.class,
    com.mortbay.HTML.Image.class,
    com.mortbay.HTML.Include.class,
    com.mortbay.HTML.Input.class,
    com.mortbay.HTML.Link.class,
    com.mortbay.HTML.List.class,
    com.mortbay.HTML.Page.class,
    com.mortbay.HTML.Script.class,
    com.mortbay.HTML.Select.class,
    com.mortbay.HTML.Style.class,
    com.mortbay.HTML.StyleLink.class,
    com.mortbay.HTML.Table.class,
    com.mortbay.HTML.TableForm.class,
    com.mortbay.HTML.Tag.class,
    com.mortbay.HTML.Text.class,
    com.mortbay.HTML.TextArea.class,
    com.mortbay.HTTP.B64Code.class,
    com.mortbay.HTTP.Configure.BaseConfiguration.class,
    com.mortbay.HTTP.Configure.FileServer.class,
    com.mortbay.HTTP.Configure.ProxyConfig.class,
    com.mortbay.HTTP.Configure.ServletServer.class,
    com.mortbay.HTTP.Cookies.class,
    com.mortbay.HTTP.Filter.GzipFilter.class,
    com.mortbay.HTTP.Filter.HtmlExpireFilter.class,
    com.mortbay.HTTP.Filter.HtmlFilter.class,
    com.mortbay.HTTP.Filter.MethodTag.class,
    com.mortbay.HTTP.Filter.TestHarness.class,
    com.mortbay.HTTP.HTML.EmbedUrl.class,
    com.mortbay.HTTP.Handler.BasicAuthHandler.class,
    com.mortbay.HTTP.Handler.BasicAuthRealm.class,
    com.mortbay.HTTP.Handler.DefaultExceptionHandler.class,
    com.mortbay.HTTP.Handler.FileHandler.class,
    com.mortbay.HTTP.Handler.FileJarServletLoader.class,
    com.mortbay.HTTP.Handler.FilterHandler.class,
    com.mortbay.HTTP.Handler.ForwardHandler.class,
    com.mortbay.HTTP.Handler.LogHandler.class,
    com.mortbay.HTTP.Handler.NotFoundHandler.class,
    com.mortbay.HTTP.Handler.NullHandler.class,
    com.mortbay.HTTP.Handler.ParamHandler.class,
    com.mortbay.HTTP.Handler.ProxyHandler.class,
    com.mortbay.HTTP.Handler.ServletContextWrapper.class,
    com.mortbay.HTTP.Handler.ServletHandler.class,
    com.mortbay.HTTP.Handler.ServletHolder.class,
    com.mortbay.HTTP.Handler.ServletLoader.class,
    com.mortbay.HTTP.Handler.SessionHandler.class,
    com.mortbay.HTTP.Handler.TerseExceptionHandler.class,
    com.mortbay.HTTP.Handler.TestHarness.class,
    com.mortbay.HTTP.Handler.TranslateHandler.class,
    com.mortbay.HTTP.Handler.VirtualHostHandler.class,
    com.mortbay.HTTP.HeadException.class,
    com.mortbay.HTTP.HttpFilter.class,
    com.mortbay.HTTP.HttpHeader.class,
    com.mortbay.HTTP.HttpInputStream.class,
    com.mortbay.HTTP.HttpListener.class,
    com.mortbay.HTTP.HttpOutputStream.class,
    com.mortbay.HTTP.HttpRequest.class,
    com.mortbay.HTTP.HttpRequestDispatcher.class,
    com.mortbay.HTTP.HttpResponse.class,
    com.mortbay.HTTP.HttpServer.class,
    com.mortbay.HTTP.HttpTests.class,
    com.mortbay.HTTP.MultiPartRequest.class,
    com.mortbay.HTTP.MultiPartResponse.class,
    com.mortbay.HTTP.PathMap.class,
    com.mortbay.HTTP.SessionContext.class,
    com.mortbay.HTTP.Version.class,
    com.mortbay.JDBC.Clause.class,
    com.mortbay.JDBC.CloudscapeAdaptor.class,
    com.mortbay.JDBC.Column.class,
    com.mortbay.JDBC.ColumnGroup.class,
    com.mortbay.JDBC.CreateTables.class,
    com.mortbay.JDBC.Database.class,
    com.mortbay.JDBC.DbAdaptor.class,
    com.mortbay.JDBC.Key.class,
    com.mortbay.JDBC.MsqlAdaptor.class,
    com.mortbay.JDBC.MultiTestTable.class,
    com.mortbay.JDBC.NonKeyException.class,
    com.mortbay.JDBC.OracleAdaptor.class,
    com.mortbay.JDBC.RelationalTable.class,
    com.mortbay.JDBC.Row.class,
    com.mortbay.JDBC.RowEnumeration.class,
    com.mortbay.JDBC.Select.class,
    com.mortbay.JDBC.Table.class,
    com.mortbay.JDBC.Transaction.class,
    com.mortbay.Jetty.Demo.class,
    com.mortbay.Jetty.DemoIndex.class,
    com.mortbay.Jetty.GenerateLafServlet.class,
    com.mortbay.Jetty.GenerateServlet.class,
    com.mortbay.Jetty.JettyLaF.class,
    com.mortbay.Jetty.MultiPartCount.class,
    com.mortbay.Jetty.Server.class,
    com.mortbay.Jetty.Server21.class,
    com.mortbay.Jetty.StressTester.class,
    com.mortbay.Jetty.UploadServlet.class,
    com.mortbay.Servlets.ConfigDump.class,
    com.mortbay.Servlets.DebugServlet.class,
    com.mortbay.Servlets.Dispatch.class,
    com.mortbay.Servlets.DispatchServlet.class,
    com.mortbay.Servlets.Dump.class,
    com.mortbay.Servlets.Exit.class,
    com.mortbay.Servlets.IndexServlet.class,
    com.mortbay.Servlets.LookAndFeelServlet.class,
    com.mortbay.Servlets.PagePush.class,
    com.mortbay.Servlets.PropertyTreeEditor.class,
    com.mortbay.Servlets.ServletDispatch.class,
    com.mortbay.Servlets.ServletNode.class,
    com.mortbay.Servlets.SessionDump.class,
    com.mortbay.Util.ArrayConverter.class,
    com.mortbay.Util.BlockingQueue.class,
    com.mortbay.Util.ContainerIteratorTransformer.class,
    com.mortbay.Util.Code.class,
    com.mortbay.Util.ConvertFail.class,
    com.mortbay.Util.ConverterSet.class,
    com.mortbay.Util.DataClass.class,
    com.mortbay.Util.DictionaryConverter.class,
    com.mortbay.Util.DumpFilterOutputStream.class,
    com.mortbay.Util.IO.class,
    com.mortbay.Util.InetAddrPort.class,
    com.mortbay.Util.InetGateway.class,
    com.mortbay.Util.LineInput.class,
    com.mortbay.Util.ObjectConverter.class,
    com.mortbay.Util.ObjectConverterTest.class,
    com.mortbay.Util.Observed.class,
    com.mortbay.Util.Password.class,
    com.mortbay.Util.PropertyEnumeration.class,
    com.mortbay.Util.PropertyTree.class,
    com.mortbay.Util.PropertyTreeTest.class,
    com.mortbay.Util.SmtpMail.class,
    com.mortbay.Util.StringUtil.class,
    com.mortbay.Util.SummaryFilterOutputStream.class,
    com.mortbay.Util.Test.T1.class,
    com.mortbay.Util.Test.T2.class,
    com.mortbay.Util.TestHarness.class,
    com.mortbay.Util.ThreadPool.class,
    com.mortbay.Util.ThreadedServer.class,
    com.mortbay.Util.URI.class,
    com.mortbay.Util.UrlEncoded.class,
    javax.servlet.GenericServlet.class,
    javax.servlet.ServletException.class,
    javax.servlet.ServletInputStream.class,
    javax.servlet.ServletOutputStream.class,
    javax.servlet.UnavailableException.class,
    javax.servlet.http.Cookie.class,
    javax.servlet.http.HttpServlet.class,
    javax.servlet.http.HttpSessionBindingEvent.class,
    javax.servlet.http.HttpUtils.class,
    javax.servlet.jsp.JspEngineInfo.class,
    javax.servlet.jsp.JspFactory.class,
    javax.servlet.jsp.JspWriter.class,
    javax.servlet.jsp.PageContext.class,
};}

