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
package com.vaadin.flow.spring.flowsecurity.views;

import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.LocaleUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "translations")
@AnonymousAllowed
public class TranslationView extends Div {

    public static final String TEST_VIEW_ID = "TranslationView";
    public static final String LOCALES_ID = "available-locales";

    public TranslationView() {
        setId(TEST_VIEW_ID);

        Span defaultLang = new Span(getTranslation("label", Locale.ENGLISH));
        defaultLang.setId("english");
        Span french = new Span(getTranslation("label", Locale.FRANCE));
        french.setId("french");
        Span japanese = new Span(getTranslation("label", Locale.JAPAN));
        japanese.setId("japanese");

        Optional<I18NProvider> i18NProvider = LocaleUtil.getI18NProvider();
        if (i18NProvider.isPresent()) {
            add(new Span("Available translation locales:"));
            StringBuilder locales = new StringBuilder();
            for (Locale locale : i18NProvider.get().getProvidedLocales()) {
                if (locales.length() > 0) {
                    locales.append(", ");
                }
                locales.append(locale.toString());
            }
            Span localeSpan = new Span(locales.toString());
            localeSpan.setId(LOCALES_ID);
            add(localeSpan, new Div());
        }
        add(defaultLang, new Div(), french, new Div(), japanese);
    }
}
