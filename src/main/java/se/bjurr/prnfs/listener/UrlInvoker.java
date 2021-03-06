package se.bjurr.prnfs.listener;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.CharStreams.readLines;
import static java.lang.Boolean.TRUE;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bjurr.prnfs.settings.Header;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.io.Closeables;

public class UrlInvoker {

 private static final Logger logger = LoggerFactory.getLogger(UrlInvoker.class);
 private String urlParam;
 private String method;
 private Optional<String> postContent;
 private final List<Header> headers = newArrayList();
 private Optional<String> proxyUser;
 private Optional<String> proxyPassword;
 private Optional<String> proxyHost;
 private Integer proxyPort;

 private UrlInvoker() {
 }

 public static UrlInvoker urlInvoker() {
  return new UrlInvoker();
 }

 public UrlInvoker withHeader(String name, String value) {
  headers.add(new Header(name, value));
  return this;
 }

 public UrlInvoker withMethod(String method) {
  this.method = method;
  return this;
 }

 public UrlInvoker withPostContent(Optional<String> postContent) {
  this.postContent = postContent;
  return this;
 }

 public UrlInvoker withUrlParam(String urlParam) {
  this.urlParam = urlParam.replaceAll("\\s", "%20");
  return this;
 }

 public void invoke() {
  InputStreamReader ir = null;
  DataOutputStream wr = null;
  try {
   logger.info("Url: \"" + urlParam + "\"");
   final URL url = new URL(urlParam);
   HttpURLConnection uc = null;
   if (shouldUseProxy()) {
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getProxyHost().get(), getProxyPort()));
    if (shouldAuthenticateProxy()) {
     Authenticator authenticator = new Authenticator() {
      @Override
      public PasswordAuthentication getPasswordAuthentication() {
       return (new PasswordAuthentication(getProxyUser().get(), getProxyPassword().get().toCharArray()));
      }
     };
     Authenticator.setDefault(authenticator);
    }
    uc = (HttpURLConnection) url.openConnection(proxy);
   } else {
    uc = (HttpURLConnection) url.openConnection();
   }
   uc.setRequestMethod(method);
   for (Header header : headers) {
    logger.info("header: \"" + header.getName() + "\" value: \"" + header.getValue() + "\"");
    uc.setRequestProperty(header.getName(), getHeaderValue(header));
   }
   uc.setDoOutput(true);
   if (shouldPostContent()) {
    logger.debug(method + " >\n" + postContent.get());
    uc.setDoInput(true);
    uc.setRequestProperty("Content-Length", postContent.get().length() + "");
    wr = new DataOutputStream(uc.getOutputStream());
    wr.write(postContent.get().getBytes(UTF_8));
   }
   ir = new InputStreamReader(uc.getInputStream(), UTF_8);
   logger.debug(on("\n").join(readLines(ir)));
  } catch (final Exception e) {
   try {
    Closeables.close(ir, TRUE);
    if (wr != null) {
     Closeables.close(wr, TRUE);
    }
   } catch (final IOException e1) {
   }
   logger.error("", e);
  }
 }

 @VisibleForTesting
 public boolean shouldAuthenticateProxy() {
  return getProxyUser().isPresent() && getProxyPassword().isPresent();
 }

 @VisibleForTesting
 public static String getHeaderValue(Header header) {
  return header.getValue();
 }

 public String getMethod() {
  return method;
 }

 public Optional<String> getPostContent() {
  return postContent;
 }

 public String getUrlParam() {
  return urlParam;
 }

 @VisibleForTesting
 public boolean shouldPostContent() {
  return (method.equals("POST") || method.equals("PUT")) && postContent.isPresent();
 }

 public List<Header> getHeaders() {
  return headers;
 }

 @VisibleForTesting
 public boolean shouldUseProxy() {
  return getProxyHost().isPresent() && getProxyPort() > 0;
 }

 public Optional<String> getProxyUser() {
  return proxyUser;
 }

 public UrlInvoker withProxyUser(Optional<String> proxyUser) {
  this.proxyUser = proxyUser;
  return this;
 }

 public Optional<String> getProxyPassword() {
  return proxyPassword;
 }

 public UrlInvoker withProxyPassword(Optional<String> proxyPassword) {
  this.proxyPassword = proxyPassword;
  return this;
 }

 public Optional<String> getProxyHost() {
  return proxyHost;
 }

 public UrlInvoker withProxyServer(Optional<String> proxyHost) {
  this.proxyHost = proxyHost;
  return this;
 }

 public Integer getProxyPort() {
  return proxyPort;
 }

 public UrlInvoker withProxyPort(Integer proxyPort) {
  this.proxyPort = proxyPort;
  return this;
 }
}
