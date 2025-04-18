/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.function.Supplier;

/**
 * Context in which {@link VaadinService} is running.
 *
 * This is used to store service-scoped attributes and also works as a wrapper
 * for context objects with properties e.g. <code>ServletContext</code> and
 * <code>PortletContext</code>
 *
 * @since 2.0.0
 */
public interface VaadinContext extends Serializable {

    /**
     * Returns value of the specified attribute, creating and storing a default
     * value if attribute not present.
     * <p>
     * If attribute is not yet available the {@code defaultValueSupplier} is
     * used to get the default value which is set as the attribute value and the
     * value is returned. The operation is executed atomically.
     *
     * @param type
     *            Type of the attribute.
     * @param defaultValueSupplier
     *            {@link Supplier} of the default value, called when there is no
     *            value already present. May be {@code null}.
     * @return Value of the specified attribute.
     */
    <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier);

    /**
     * Returns value of the specified attribute.
     *
     * @param type
     *            Type of the attribute.
     * @return Value of the specified attribute.
     */
    default <T> T getAttribute(Class<T> type) {
        return getAttribute(type, null);
    }

    /**
     * Sets the attribute value for the give type, overriding previously
     * existing one. Values are based on exact type, meaning only one attribute
     * of given type is possible at any given time.
     *
     * @param clazz
     *            the type to associate the value with, not <code>null</code>
     *
     * @param value
     *            the attribute value to set, or <code>null</code> to remove the
     *            current value
     * @since
     */
    <T> void setAttribute(Class<T> clazz, T value);

    /**
     * Sets the attribute value, overriding previously existing one. Values are
     * based on exact type, meaning only one attribute of given type is possible
     * at any given time.
     *
     * @param value
     *            attribute value, not {@code null}.
     * @see #removeAttribute(Class) for removing attributes.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    default void setAttribute(Object value) {
        assert value != null;

        Class clazz = value.getClass();
        setAttribute(clazz, value);
    }

    /**
     * Removes an attribute identified by the given type. If the attribute does
     * not exist, no action will be taken.
     *
     * @param clazz
     *            Attribute type.
     *
     * @see #setAttribute(Object) for setting attributes.
     */
    void removeAttribute(Class<?> clazz);

    /**
     * Returns the names of the initialization parameters as an
     * <code>Enumeration</code>, or an empty <code>Enumeration</code> if there
     * are o initialization parameters.
     *
     * @return initialization parameters as a <code>Enumeration</code>
     */
    Enumeration<String> getContextParameterNames();

    /**
     * Returns the value for the requested parameter, or <code>null</code> if
     * the parameter does not exist.
     *
     * @param name
     *            name of the parameter whose value is requested
     * @return parameter value as <code>String</code> or <code>null</code> for
     *         no parameter
     */
    String getContextParameter(String name);
}
