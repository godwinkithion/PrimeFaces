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
package org.primefaces.component.menu;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.primefaces.component.api.AjaxSource;
import org.primefaces.component.api.UIOutcomeTarget;
import org.primefaces.context.RequestContext;
import org.primefaces.event.MenuActionEvent;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuItem;
import org.primefaces.model.menu.Separator;
import org.primefaces.model.menu.Submenu;
import org.primefaces.renderkit.OutcomeTargetRenderer;
import org.primefaces.util.AjaxRequestBuilder;
import org.primefaces.util.ComponentUtils;
import org.primefaces.util.WidgetBuilder;

public abstract class BaseMenuRenderer extends OutcomeTargetRenderer {
    
    public final static String SEPARATOR = "_";
    
    @Override
	public void decode(FacesContext context, UIComponent component) {
		AbstractMenu menu = (AbstractMenu) component;
        String clientId = menu.getClientId(context);
        Map<String,String> params = context.getExternalContext().getRequestParameterMap();
        
        if(params.containsKey(clientId)) {
            String menuid = params.get(clientId + "_menuid");
            MenuItem menuitem = findMenuitem(menu.getElements(), menuid);
            MenuActionEvent event = new MenuActionEvent(menu, menuitem);
            
            menu.queueEvent(event);
        }
	}
    
    protected MenuItem findMenuitem(List<MenuElement> elements, String id) {        
        if(elements == null || elements.isEmpty()) {
            return null;
        }
        else {
            String[] paths = id.split(SEPARATOR);
            
            if(paths.length == 0)
                return null;
            
            int childIndex = Integer.parseInt(paths[0]);
            if(childIndex >= elements.size()) 
                return null;
            
            MenuElement childElement = elements.get(childIndex);

            if(paths.length == 1) {
                return (MenuItem) childElement;
            } 
            else {
                String relativeIndex = id.substring(id.indexOf(SEPARATOR) + 1);

                return findMenuitem(((Submenu) childElement).getElements(), relativeIndex);
            }
        }
    }
    
    @Override
	public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
		AbstractMenu menu = (AbstractMenu) component;

		encodeMarkup(context, menu);
		encodeScript(context, menu);
	}

    protected abstract void encodeMarkup(FacesContext context, AbstractMenu abstractMenu) throws IOException;

    protected abstract void encodeScript(FacesContext context, AbstractMenu abstractMenu) throws IOException;

    protected void encodeMenuItem(FacesContext context, AbstractMenu menu, MenuItem menuitem) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String icon = menuitem.getIcon();
        String title = menuitem.getTitle();

		if(menuitem.shouldRenderChildren()) {
			renderChildren(context, (UIComponent) menuitem);
		}
        else {
            boolean disabled = menuitem.isDisabled();
            String onclick = menuitem.getOnclick();
            
            writer.startElement("a", null);
            if(title != null) {
                writer.writeAttribute("title", title, null);
            }
            
            String styleClass = menuitem.getStyleClass();
            styleClass = styleClass == null ? AbstractMenu.MENUITEM_LINK_CLASS : AbstractMenu.MENUITEM_LINK_CLASS + " " + styleClass;
            styleClass = disabled ? styleClass + " ui-state-disabled" : styleClass;
            
            writer.writeAttribute("class", styleClass, null);
            
            if(menuitem.getStyle() != null) {
                writer.writeAttribute("style", menuitem.getStyle(), null);
            }
                  
            //GET
			if(menuitem.getUrl() != null || menuitem.getOutcome() != null) {                
                String targetURL = getTargetURL(context, (UIOutcomeTarget) menuitem);
                String href = disabled ? "javascript:void(0)" : targetURL;
				writer.writeAttribute("href", href, null);
                                
				if(menuitem.getTarget() != null) {
                    writer.writeAttribute("target", menuitem.getTarget(), null);
                }
			}
            //POST
            else {
				writer.writeAttribute("href", "javascript:void(0)", null);
                
                UIComponent form = ComponentUtils.findParentForm(context, menu);
                if(form == null) {
                    throw new FacesException("MenuItem must be inside a form element");
                }

                String command = null;
                if(menuitem.isDynamic()) {
                    String menuClientId = menu.getClientId(context);
                    Map<String,Object> params = new HashMap<String,Object>();
                    params.put(menuClientId + "_menuid", menuitem.getId());

                    command = menuitem.isAjax() ? buildAjaxRequest(context, menu, (AjaxSource) menuitem, form, params) : buildNonAjaxRequest(context, menu, form, menuClientId, params, true);
                } 
                else {
                    command = menuitem.isAjax() ? buildAjaxRequest(context, (AjaxSource) menuitem, form) : buildNonAjaxRequest(context, ((UIComponent) menuitem), form, ((UIComponent) menuitem).getClientId(context), true);
                }

                onclick = (onclick == null) ? command : onclick + ";" + command;
			}

            if(onclick != null && !disabled) {
                writer.writeAttribute("onclick", onclick, null);
            }
 
            if(icon != null) {
                writer.startElement("span", null);
                writer.writeAttribute("class", AbstractMenu.MENUITEM_ICON_CLASS + " " + icon, null);
                writer.endElement("span");
            }

			if(menuitem.getValue() != null) {
                writer.startElement("span", null);
                writer.writeAttribute("class", AbstractMenu.MENUITEM_TEXT_CLASS, null);
                writer.writeText((String) menuitem.getValue(), "value");
                writer.endElement("span");
            }

            writer.endElement("a");
		}
	}

    protected void encodeSeparator(FacesContext context, Separator separator) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String style = separator.getStyle();
        String styleClass = separator.getStyleClass();
        styleClass = styleClass == null ? Menu.SEPARATOR_CLASS : Menu.SEPARATOR_CLASS + " " + styleClass;

        //title
        writer.startElement("li", null);
        writer.writeAttribute("class", styleClass, null);
        if(style != null) {
            writer.writeAttribute("style", style, null);
        }
        
        writer.endElement("li");
	}
    
    protected void encodeOverlayConfig(FacesContext context, OverlayMenu menu, WidgetBuilder wb) throws IOException {

        wb.attr("overlay", true)
            .attr("my", menu.getMy())
            .attr("at", menu.getAt());
        
        UIComponent trigger = ((UIComponent) menu).findComponent(menu.getTrigger());
        String triggerClientId = trigger == null ? menu.getTrigger() : trigger.getClientId(context);

        wb.attr("trigger", triggerClientId)
            .attr("triggerEvent", menu.getTriggerEvent());
    }
 
    @Override
	public void encodeChildren(FacesContext facesContext, UIComponent component) throws IOException {
		//Do nothing
	}

    @Override
	public boolean getRendersChildren() {
		return true;
	}
    
    protected String buildAjaxRequest(FacesContext context, AbstractMenu menu, AjaxSource source, UIComponent form, Map<String,Object> params) {
        String clientId = menu.getClientId(context);
        
        AjaxRequestBuilder builder = RequestContext.getCurrentInstance().getAjaxRequestBuilder();
        
        builder.init()
        		.source(clientId)
                .process(menu, source.getProcess())
                .update(menu, source.getUpdate())
                .async(source.isAsync())
                .global(source.isGlobal())
                .partialSubmit(source.isPartialSubmit(), source.isPartialSubmitSet())
                .onstart(source.getOnstart())
                .onerror(source.getOnerror())
                .onsuccess(source.getOnsuccess())
                .oncomplete(source.getOncomplete())
                .params(params);
        
        if(form != null) {
            builder.form(form.getClientId(context));
        }
        
        builder.preventDefault();
                
        return builder.build();
    }
    
    protected String buildNonAjaxRequest(FacesContext context, UIComponent component, UIComponent form, String decodeParam, Map<String,Object> parameters, boolean submit) {		
        StringBuilder request = new StringBuilder();
        String formId = form.getClientId(context);
        Map<String,Object> params = new HashMap<String, Object>();
        
        if(decodeParam != null) {
            params.put(decodeParam, decodeParam);
        }
        
		for(UIComponent child : component.getChildren()) {
			if(child instanceof UIParameter) {
                UIParameter param = (UIParameter) child;

                params.put(param.getName(), param.getValue());
			}
		}
        
        if(parameters != null && !parameters.isEmpty()) {
            params.putAll(parameters);
        }
        
        //append params
        if(!params.isEmpty()) {
            request.append("PrimeFaces.addSubmitParam('").append(formId).append("',{");
            
            for(Iterator<String> it = params.keySet().iterator(); it.hasNext();) {
                String key = it.next();
                Object value = params.get(key);

                request.append("'").append(key).append("':'").append(value).append("'");

                if(it.hasNext())
                    request.append(",");
            }
            
            request.append("})");
        }
        
        if(submit) {
            request.append(".submit('").append(formId).append("');");
        }
		
		return request.toString();
	}
}
