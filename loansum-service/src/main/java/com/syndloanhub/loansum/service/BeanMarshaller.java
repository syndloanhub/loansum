
/**
 * Copyright (c) 2018 SyndLoanHub, LLC and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License from within this distribution and at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.syndloanhub.loansum.service;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.joda.beans.Bean;
import org.joda.beans.ser.JodaBeanSer;

import com.opengamma.strata.collect.result.Result;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class BeanMarshaller implements
    MessageBodyWriter<Bean>,
    MessageBodyReader<Bean> {

  @Override
  public long getSize(Bean arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
    return -1;
  }

  @Override
  public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
    return Bean.class.isAssignableFrom(arg0);
  }

  @Override
  public void writeTo(Bean bean, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
      MultivaluedMap<String, Object> arg5, OutputStream stream) throws IOException, WebApplicationException {
    DataOutputStream outputStream = new DataOutputStream(stream);
    outputStream.writeBytes(JodaBeanSer.PRETTY.jsonWriter().write(bean));
  }

  @Override
  public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
    return Bean.class.isAssignableFrom(arg0);
  }

  @Override
  public Result<Bean> readFrom(Class<Bean> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
      MultivaluedMap<String, String> arg4, InputStream arg5) throws IOException, WebApplicationException {
    InputStreamReader reader = new InputStreamReader(arg5);
    return readLoanTrade(reader);
  }

  private Result<Bean> readLoanTrade(InputStreamReader reader) {
    try {
      System.out.println("reading loan trade");
      return Result.success(JodaBeanSer.PRETTY.jsonReader().read(reader));
    } catch (Exception exc) {
      return Result.failure(exc);
    }
  }

}
