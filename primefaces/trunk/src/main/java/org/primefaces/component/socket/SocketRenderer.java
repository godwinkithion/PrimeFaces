/*
 * Copyright 2009-2013 PrimeTek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.primefaces.component.socket;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.primefaces.context.RequestContext;
import org.primefaces.renderkit.CoreRenderer;
import org.primefaces.util.Constants;
import org.primefaces.util.WidgetBuilder;

public class SocketRenderer extends CoreRenderer {

	@Override
	public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
		Socket socket = (Socket) component;
        String channel = socket.getChannel();
        String channelUrl = Constants.PUSH_PATH + channel;
        String url = getResourceURL(context, channelUrl);
        String pushServer = RequestContext.getCurrentInstance().getApplicationContext().getConfig().getPushServerURL();
        String clientId = socket.getClientId(context);
        
        if(pushServer != null) {
            url = pushServer + url;
        }

        WidgetBuilder wb = getWidgetBuilder(context);
        wb.widget("Socket", socket.resolveWidgetVar(), clientId, true);
        
        wb.attr("url", url)
        	.attr("autoConnect", socket.isAutoConnect())
        	.attr("transport", socket.getTransport())
        	.attr("fallbackTransport", socket.getFallbackTransport())
        	.attr("onMessage", socket.getOnMessage(), null)
        	.attr("onError", socket.getOnError(), null);

        encodeClientBehaviors(context, socket, wb);

        startScript(writer, clientId);
        writer.write(wb.build());
        endScript(writer);
	}
}
