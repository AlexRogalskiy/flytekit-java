/*
 * Copyright 2021 Flyte Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.flyte.flytekit;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.flyte.api.v1.Binding;
import org.flyte.api.v1.BindingData;
import org.flyte.api.v1.Literal;
import org.flyte.api.v1.Node;
import org.flyte.api.v1.PartialTaskIdentifier;
import org.flyte.api.v1.Primitive;
import org.flyte.api.v1.Scalar;
import org.flyte.api.v1.TaskNode;
import org.junit.jupiter.api.Test;

class SdkRemoteTaskTest {

  @Test
  void applyShouldReturnASdkTaskNode() {
    Map<String, SdkBindingData> inputs = new HashMap<>();
    inputs.put("a", SdkBindingData.ofInteger(1));
    inputs.put("b", SdkBindingData.ofString("2"));
    SdkRemoteTask<Map<String, Literal>, Map<String, Literal>> remoteTask = new TestSdkRemoteTask();

    SdkNode node =
        remoteTask.apply(
            mock(SdkWorkflowBuilder.class),
            "lookup-endsong",
            singletonList("upstream-1"),
            /*metadata=*/ null,
            inputs);

    assertAll(
        () -> assertThat(node.getNodeId(), is("lookup-endsong")),
        () ->
            assertThat(
                node.toIdl(),
                is(
                    Node.builder()
                        .id("lookup-endsong")
                        .taskNode(
                            TaskNode.builder()
                                .referenceId(
                                    PartialTaskIdentifier.builder()
                                        .domain("dev")
                                        .project("project-a")
                                        .name("LookupTask")
                                        .version("version")
                                        .build())
                                .build())
                        .upstreamNodeIds(singletonList("upstream-1"))
                        .inputs(
                            Arrays.asList(
                                Binding.builder()
                                    .var_("a")
                                    .binding(
                                        BindingData.ofScalar(
                                            Scalar.ofPrimitive(Primitive.ofIntegerValue(1))))
                                    .build(),
                                Binding.builder()
                                    .var_("b")
                                    .binding(
                                        BindingData.ofScalar(
                                            Scalar.ofPrimitive(Primitive.ofStringValue("2"))))
                                    .build()))
                        .build())),
        () ->
            assertThat(
                node.getOutputs(),
                is(
                    singletonMap(
                        "c",
                        SdkBindingData.ofOutputReference(
                            "lookup-endsong", "c", LiteralTypes.BOOLEAN)))));
  }

  @SuppressWarnings("ExtendsAutoValue")
  static class TestSdkRemoteTask extends SdkRemoteTask<Map<String, Literal>, Map<String, Literal>> {

    @Override
    public String domain() {
      return "dev";
    }

    @Override
    public String project() {
      return "project-a";
    }

    @Override
    public String name() {
      return "LookupTask";
    }

    @Override
    public String version() {
      return "version";
    }

    @Override
    public SdkType<Map<String, Literal>> inputs() {
      return TestSdkType.of("a", LiteralTypes.INTEGER, "b", LiteralTypes.STRING);
    }

    @Override
    public SdkType<Map<String, Literal>> outputs() {
      return TestSdkType.of("c", LiteralTypes.BOOLEAN);
    }
  }
}
