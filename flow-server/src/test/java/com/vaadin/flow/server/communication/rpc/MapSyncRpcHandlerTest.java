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
package com.vaadin.flow.server.communication.rpc;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ModelList;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapSyncRpcHandlerTest {

    private static final String NEW_VALUE = "newValue";
    private static final String DUMMY_EVENT = "dummy-event";
    private static final String TEST_PROPERTY = "test-property";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Tag(Tag.A)
    public static class TestComponent extends Component {

    }

    @Test
    public void testSynchronizeProperty() throws Exception {
        TestComponent c = new TestComponent();
        Element element = c.getElement();
        ElementPropertyMap.getModel(element.getNode())
                .setUpdateFromClientFilter(name -> true);

        UI ui = new UI();
        ui.add(c);
        Assert.assertFalse(element.hasProperty(TEST_PROPERTY));
        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, "value1");
        Assert.assertEquals("value1", element.getPropertyRaw(TEST_PROPERTY));
        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, "value2");
        Assert.assertEquals("value2", element.getPropertyRaw(TEST_PROPERTY));
    }

    @Test
    public void syncJSON_jsonIsForStateNodeInList_propertySetToStateNodeCopy()
            throws Exception {
        // Let's use element's ElementPropertyMap for testing.
        TestComponent component = new TestComponent();
        Element element = component.getElement();
        UI ui = new UI();
        ui.add(component);

        StateNode node = element.getNode();

        // Set model value directly via ElementPropertyMap
        ElementPropertyMap propertyMap = node
                .getFeature(ElementPropertyMap.class);
        propertyMap.setUpdateFromClientFilter(name -> true);
        ModelList modelList = propertyMap.resolveModelList("foo");
        // fake StateNode has been created for the model
        StateNode item = new StateNode(ElementPropertyMap.class);
        modelList.add(item);
        item.getFeature(ElementPropertyMap.class).setProperty("bar", "baz");

        // Use the model node id for JSON object which represents a value to
        // update
        JsonObject json = Json.createObject();
        json.put("nodeId", item.getId());

        // send sync request
        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, json);

        // Now the model node should be copied and available as the
        // TEST_PROPERTY value
        Serializable testPropertyValue = propertyMap.getProperty(TEST_PROPERTY);

        Assert.assertTrue(testPropertyValue instanceof StateNode);

        StateNode newNode = (StateNode) testPropertyValue;
        Assert.assertNotEquals(item.getId(), newNode.getId());

        Assert.assertEquals("baz", newNode.getFeature(ElementPropertyMap.class)
                .getProperty("bar"));
    }

    @Test
    public void syncJSON_jsonIsPropertyValueOfStateNode_propertySetToNode()
            throws Exception {
        // Let's use element's ElementPropertyMap for testing.
        TestComponent component = new TestComponent();
        Element element = component.getElement();
        UI ui = new UI();
        ui.add(component);

        StateNode node = element.getNode();

        // Set model value directly via ElementPropertyMap
        ElementPropertyMap propertyMap = node
                .getFeature(ElementPropertyMap.class);
        propertyMap.setUpdateFromClientFilter(name -> true);

        ElementPropertyMap modelMap = propertyMap.resolveModelMap("foo");
        // fake StateNode has been created for the model
        StateNode model = modelMap.getNode();
        modelMap.setProperty("bar", "baz");

        // Use the model node id for JSON object which represents a value to
        // update
        JsonObject json = Json.createObject();
        json.put("nodeId", model.getId());

        // send sync request
        sendSynchronizePropertyEvent(element, ui, "foo", json);

        Serializable testPropertyValue = propertyMap.getProperty("foo");

        Assert.assertTrue(testPropertyValue instanceof StateNode);

        StateNode newNode = (StateNode) testPropertyValue;
        Assert.assertSame(model, newNode);
    }

    @Test
    public void syncJSON_jsonIsNotListItemAndNotPropertyValue_propertySetToJSON()
            throws Exception {
        // Let's use element's ElementPropertyMap for testing.
        TestComponent component = new TestComponent();
        Element element = component.getElement();
        UI ui = new UI();
        ui.add(component);

        StateNode node = element.getNode();

        TestComponent anotherComonent = new TestComponent();
        StateNode anotherNode = anotherComonent.getElement().getNode();

        ElementPropertyMap.getModel(node)
                .setUpdateFromClientFilter(name -> true);

        // Use the model node id for JSON object which represents a value to
        // update
        JsonObject json = Json.createObject();
        json.put("nodeId", anotherNode.getId());

        // send sync request
        sendSynchronizePropertyEvent(element, ui, "foo", json);

        Serializable testPropertyValue = node
                .getFeature(ElementPropertyMap.class).getProperty("foo");

        Assert.assertNotSame(anotherNode, testPropertyValue);
        Assert.assertTrue(testPropertyValue instanceof JsonValue);
    }

    @Test
    public void disabledElement_updateDisallowed_updateIsNotDone()
            throws Exception {
        Element element = ElementFactory.createDiv();
        UI ui = new UI();
        ui.getElement().appendChild(element);

        element.setEnabled(false);
        element.addPropertyChangeListener(TEST_PROPERTY, DUMMY_EVENT, event -> {
        }).setDisabledUpdateMode(DisabledUpdateMode.ONLY_WHEN_ENABLED);

        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, NEW_VALUE);

        Assert.assertNotEquals(NEW_VALUE,
                element.getPropertyRaw(TEST_PROPERTY));
    }

    @Test
    public void disabledElement_updateIsAllowedBySynchronizeProperty_updateIsDone()
            throws Exception {
        Element element = ElementFactory.createDiv();
        UI ui = new UI();
        ui.getElement().appendChild(element);

        element.setEnabled(false);
        element.addPropertyChangeListener(TEST_PROPERTY, DUMMY_EVENT, event -> {
        }).setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, NEW_VALUE);

        Assert.assertEquals(NEW_VALUE, element.getPropertyRaw(TEST_PROPERTY));
    }

    @Test
    public void disabledElement_updateIsAllowedByEventListener_updateIsDone()
            throws Exception {
        Element element = ElementFactory.createDiv();
        UI ui = new UI();
        ui.getElement().appendChild(element);

        element.setEnabled(false);
        element.addEventListener(DUMMY_EVENT, event -> {
        }).synchronizeProperty(TEST_PROPERTY)
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, NEW_VALUE);

        Assert.assertEquals(NEW_VALUE, element.getPropertyRaw(TEST_PROPERTY));
    }

    @Test
    public void implicitlyDisabledElement_updateIsAllowedBySynchronizeProperty_updateIsDone()
            throws Exception {
        Element element = ElementFactory.createDiv();
        UI ui = new UI();
        ui.getElement().appendChild(element);

        ui.setEnabled(false);
        element.addPropertyChangeListener(TEST_PROPERTY, DUMMY_EVENT, event -> {
        }).setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, NEW_VALUE);

        Assert.assertEquals(NEW_VALUE, element.getPropertyRaw(TEST_PROPERTY));
    }

    @Test
    public void implicitlyDisabledElement_updateIsAllowedByEventListener_updateIsDone()
            throws Exception {
        Element element = ElementFactory.createDiv();
        UI ui = new UI();
        ui.getElement().appendChild(element);

        ui.setEnabled(false);
        element.addEventListener(DUMMY_EVENT, event -> {
        }).synchronizeProperty(TEST_PROPERTY)
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, NEW_VALUE);

        Assert.assertEquals(NEW_VALUE, element.getPropertyRaw(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noSyncPropertiesFeature_noExplicitAllow_throws() {
        StateNode noSyncProperties = new StateNode(ElementPropertyMap.class);

        ElementPropertyMap map = noSyncProperties
                .getFeature(ElementPropertyMap.class);

        new MapSyncRpcHandler().handleNode(noSyncProperties,
                createSyncPropertyInvocation(noSyncProperties, TEST_PROPERTY,
                        NEW_VALUE));

        Assert.assertEquals(NEW_VALUE, map.getProperty(TEST_PROPERTY));
    }

    @Test
    public void propertyIsNotExplicitlyAllowed_throwsWithElementTagInfo() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(CoreMatchers.allOf(
                CoreMatchers.containsString("Element with tag 'foo'"),
                CoreMatchers.containsString("'" + TEST_PROPERTY + "'")));
        Element element = new Element("foo");

        new MapSyncRpcHandler().handleNode(element.getNode(),
                createSyncPropertyInvocation(element.getNode(), TEST_PROPERTY,
                        NEW_VALUE));
    }

    @Test
    public void propertyIsNotExplicitlyAllowed_throwsWithComponentInfo() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(CoreMatchers.allOf(
                CoreMatchers.containsString(
                        "Component " + TestComponent.class.getName()),
                CoreMatchers.containsString("'" + TEST_PROPERTY + "'")));
        TestComponent component = new TestComponent();
        Element element = component.getElement();

        new MapSyncRpcHandler().handleNode(element.getNode(),
                createSyncPropertyInvocation(element.getNode(), TEST_PROPERTY,
                        NEW_VALUE));
    }

    @Test
    public void propertyIsNotExplicitlyAllowed_subproperty_throwsWithComponentInfo() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(CoreMatchers.allOf(
                CoreMatchers.containsString(
                        "Component " + TestComponent.class.getName()),
                CoreMatchers.containsString("'" + TEST_PROPERTY + "'")));
        TestComponent component = new TestComponent();
        Element element = component.getElement();

        StateNode node = element.getNode();
        ElementPropertyMap propertyMap = node
                .getFeature(ElementPropertyMap.class)
                .resolveModelMap("foo.bar");

        new MapSyncRpcHandler().handleNode(propertyMap.getNode(),
                createSyncPropertyInvocation(propertyMap.getNode(),
                        TEST_PROPERTY, NEW_VALUE));
    }

    @Test
    public void handleNode_callsElementPropertyMapDeferredUpdateFromClient() {
        AtomicInteger deferredUpdateInvocations = new AtomicInteger();
        AtomicReference<String> deferredKey = new AtomicReference<>();
        StateNode node = new StateNode(ElementPropertyMap.class) {

            private ElementPropertyMap map = new ElementPropertyMap(this) {
                @Override
                public Runnable deferredUpdateFromClient(String key,
                        Serializable value) {
                    deferredUpdateInvocations.incrementAndGet();
                    deferredKey.set(key);
                    return () -> {
                    };
                }
            };

            @Override
            public <F extends NodeFeature> F getFeature(Class<F> featureType) {
                if (featureType.equals(ElementPropertyMap.class)) {
                    return featureType.cast(map);
                }
                return super.getFeature(featureType);
            }

        };

        new MapSyncRpcHandler().handleNode(node,
                createSyncPropertyInvocation(node, TEST_PROPERTY, NEW_VALUE));

        Assert.assertEquals(1, deferredUpdateInvocations.get());
        Assert.assertEquals(TEST_PROPERTY, deferredKey.get());
    }

    @Test
    public void handleNode_stateNodePropertyDefaultValueNotSet_doesNotWarnForUnsetDisabledPropertyChange() {

        ElementPropertyMap map = mock(ElementPropertyMap.class);
        when(map.getProperty(anyString())).thenReturn(null);
        StateNode disabledNode = mock(StateNode.class);
        when(disabledNode.getFeatureIfInitialized(ElementPropertyMap.class))
                .thenReturn(Optional.of(map));
        when(disabledNode.isEnabled()).thenReturn(false);

        Logger logger = spy(Logger.class);
        try (MockedStatic<LoggerFactory> mockedLoggerFactory = mockStatic(
                LoggerFactory.class)) {
            mockedLoggerFactory.when(
                    () -> LoggerFactory.getLogger(MapSyncRpcHandler.class))
                    .thenReturn(logger);

            new MapSyncRpcHandler().handleNode(disabledNode,
                    createSyncPropertyInvocation(disabledNode, TEST_PROPERTY,
                            NEW_VALUE));

            verify(logger, times(0)).warn(anyString(), anyString());
            verify(logger, times(1)).debug(anyString(), anyString());
        }
    }

    @Test
    public void handleNode_stateNodePropertyDefaultValueSet_warnsForSetDisabledPropertyChange() {

        ElementPropertyMap map = mock(ElementPropertyMap.class);
        when(map.getProperty(anyString())).thenReturn(NEW_VALUE);
        StateNode disabledNode = mock(StateNode.class);
        when(disabledNode.getFeatureIfInitialized(ElementPropertyMap.class))
                .thenReturn(Optional.of(map));
        when(disabledNode.isEnabled()).thenReturn(false);

        Logger logger = spy(Logger.class);
        try (MockedStatic<LoggerFactory> mockedLoggerFactory = mockStatic(
                LoggerFactory.class)) {
            mockedLoggerFactory.when(
                    () -> LoggerFactory.getLogger(MapSyncRpcHandler.class))
                    .thenReturn(logger);

            new MapSyncRpcHandler().handleNode(disabledNode,
                    createSyncPropertyInvocation(disabledNode, TEST_PROPERTY,
                            NEW_VALUE));

            verify(logger, times(1)).warn(anyString(), anyString());
            verify(logger, times(0)).debug(anyString(), anyString());
        }
    }

    private static void sendSynchronizePropertyEvent(Element element, UI ui,
            String eventType, Serializable value) throws Exception {
        new MapSyncRpcHandler().handle(ui,
                createSyncPropertyInvocation(element, eventType, value));
    }

    private static JsonObject createSyncPropertyInvocation(Element element,
            String property, Serializable value) {
        return createSyncPropertyInvocation(element.getNode(), property, value);
    }

    private static JsonObject createSyncPropertyInvocation(StateNode node,
            String property, Serializable value) {
        // Copied from ServerConnector
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_FEATURE,
                NodeFeatureRegistry.getId(ElementPropertyMap.class));
        message.put(JsonConstants.RPC_PROPERTY, property);
        message.put(JsonConstants.RPC_PROPERTY_VALUE,
                JsonCodec.encodeWithoutTypeInfo(value));

        return message;
    }
}
