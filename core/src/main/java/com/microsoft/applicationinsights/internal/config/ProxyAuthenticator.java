/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.config;

import com.microsoft.applicationinsights.internal.util.Constants;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gupele on 9/6/2016.
 */
@XmlRootElement(name="ProxyAuthenticator")
public class ProxyAuthenticator {
    private String host;
    private String port;
    private String realm;
    private String schema;
    private String user;
    private String password;

    @XmlAttribute
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @XmlAttribute
    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @XmlAttribute
    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @XmlAttribute
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @XmlAttribute
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @XmlAttribute
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDataInMap(HashMap<String, String> data) {
        setInMap(Constants.PROXY_AUTH_PORT, port, data);
        setInMap(Constants.PROXY_AUTH_HOST, host, data);
        setInMap(Constants.PROXY_AUTH_REALM, realm, data);
        setInMap(Constants.PROXT_AUTH_SCHMA, schema, data);
        setInMap(Constants.PROXY_AUTH_USER, user, data);
        setInMap(Constants.PROXY_AUTH_PASS, password, data);
    }

    private static void setInMap(String name, String value, Map<String, String> data) {
        if (!LocalStringsUtils.isNullOrEmpty(value)) {
            data.put(name, value);
        }
    }
}
