package fr.frogdevelopment.jenkins.plugins.mq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import fr.frogdevelopment.jenkins.plugins.mq.RabbitMqBuilder.Configs;
import fr.frogdevelopment.jenkins.plugins.mq.RabbitMqBuilder.RabbitConfig.RabbitConfigDescriptor;
import fr.frogdevelopment.jenkins.plugins.mq.RabbitMqBuilder.RabbitMqDescriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static fr.frogdevelopment.jenkins.plugins.mq.RabbitMqBuilder.RabbitConfig;

// https://wiki.jenkins.io/display/JENKINS/Unit+Test
public class RabbitMqBuilderTest {

    private static final RabbitConfig RABBIT_CONFIG = new RabbitConfig("rabbit-test", "roger-rabbit", 5672, "guest", "guest");

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void test_auto_magic_methods() {
        // DATA
        String rabbitName = "test-name";
        String exchange = "test-exchange";
        String routingKey = "test-routingKey";
        String parameters = "test-parameters";
        boolean isToJson = true;

        // RABBIT CONFIG
        ArrayList<RabbitConfig> rabbitConfigs = new ArrayList<>();
        rabbitConfigs.add(RABBIT_CONFIG);

        RabbitMqBuilder rabbitMqBuilder = new RabbitMqBuilder(rabbitName, exchange, parameters);
        rabbitMqBuilder.setRoutingKey(routingKey);
        rabbitMqBuilder.setToJson(isToJson);
        Configs configs = new Configs(rabbitConfigs);
        RabbitMqDescriptor descriptor = rabbitMqBuilder.getDescriptor();
        descriptor.setConfigs(configs);

        // ASSERTIONS
        Assertions.assertThat(rabbitMqBuilder.getRabbitName()).isEqualTo(rabbitName);
        Assertions.assertThat(rabbitMqBuilder.getExchange()).isEqualTo(exchange);
        Assertions.assertThat(rabbitMqBuilder.getRoutingKey()).isEqualTo(routingKey);
        Assertions.assertThat(rabbitMqBuilder.getData()).isEqualTo(parameters);
        Assertions.assertThat(rabbitMqBuilder.isToJson()).isEqualTo(isToJson);

        ListBoxModel listBoxModel = descriptor.doFillRabbitNameItems();
        Assertions.assertThat(listBoxModel).hasSameSizeAs(rabbitConfigs);
        Assertions.assertThat(listBoxModel.get(0).name).isEqualTo(RABBIT_CONFIG.getName());

        FormValidation doCheckParameters = descriptor.doCheckParameters("key_1=value_1\nkey_2=value_2");
        Assertions.assertThat(doCheckParameters.kind).isEqualTo(FormValidation.Kind.OK);

        doCheckParameters = descriptor.doCheckParameters("=empty");
        Assertions.assertThat(doCheckParameters.kind).isEqualTo(FormValidation.Kind.ERROR);

        doCheckParameters = descriptor.doCheckParameters("incorrect:format");
        Assertions.assertThat(doCheckParameters.kind).isEqualTo(FormValidation.Kind.ERROR);

        doCheckParameters = descriptor.doCheckParameters("");
        Assertions.assertThat(doCheckParameters.kind).isEqualTo(FormValidation.Kind.ERROR);

        doCheckParameters = descriptor.doCheckParameters(null);
        Assertions.assertThat(doCheckParameters.kind).isEqualTo(FormValidation.Kind.ERROR);

        Assertions.assertThat(descriptor.getConfigs()).isEqualTo(configs);
        RabbitConfigDescriptor rabbitConfigDescriptor = RABBIT_CONFIG.getDescriptor();

        FormValidation doCheckPort = rabbitConfigDescriptor.doCheckPort("132");
        Assertions.assertThat(doCheckPort.kind).isEqualTo(FormValidation.Kind.OK);

        doCheckPort = rabbitConfigDescriptor.doCheckPort("aaa");
        Assertions.assertThat(doCheckPort.kind).isEqualTo(FormValidation.Kind.ERROR);

        doCheckPort = rabbitConfigDescriptor.doCheckPort("");
        Assertions.assertThat(doCheckPort.kind).isEqualTo(FormValidation.Kind.ERROR);

        doCheckPort = rabbitConfigDescriptor.doCheckPort(null);
        Assertions.assertThat(doCheckPort.kind).isEqualTo(FormValidation.Kind.ERROR);
    }

    @Test
    public void test_unknown_RabbitConfig() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Unit_Test");

        // RABBIT CONFIG
        ArrayList<RabbitConfig> rabbitConfigs = new ArrayList<>();
        rabbitConfigs.add(RABBIT_CONFIG);
        Configs configs = new Configs(rabbitConfigs);

        RabbitMqBuilder rabbitMqBuilder = new RabbitMqBuilder("rabbit-ko", "exchange", "key=value");
        rabbitMqBuilder.setRoutingKey("key");
        rabbitMqBuilder.setToJson(true);
        rabbitMqBuilder.getDescriptor().setConfigs(configs);

        project.getBuildersList().add(rabbitMqBuilder);

        // LAUNCH BUILD
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // GET OUTPUT
        String console = FileUtils.readFileToString(build.getLogFile());

        // ASSERTIONS
        Assertions.assertThat(console).containsSubsequence(
                "Initialisation Rabbit-MQ",
                "Error while sending to Rabbit-MQ : IllegalArgumentException: Unknown rabbit config : rabbit-ko",
                "Build step 'Publish to Rabbit-MQ' marked build as failure",
                "Finished: FAILURE");
    }

    @Test
    public void test_with_build_parameter_to_json() throws IOException, ExecutionException, InterruptedException {
        RabbitMqFactory.mockRabbitTemplate = null; // to use a new one

        String exchange = "FD-exchange";
        String routingKey = "frogdevelopment.test";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Unit_Test");

        // BUILD PARAMETERS
        List<ParameterValue> parameters = new ArrayList<>();
        parameters.add(new StringParameterValue("VALUE_NAME", "value_test"));
        parameters.add(new StringParameterValue("VALUE_EMPTY", ""));
        parameters.add(new StringParameterValue("VALUE_NULL", null));

        // RABBIT CONFIG
        ArrayList<RabbitConfig> rabbitConfigs = new ArrayList<>();
        rabbitConfigs.add(RABBIT_CONFIG);

        String data = "key_1=${VALUE_NAME}";
        RabbitMqBuilder rabbitMqBuilder = new RabbitMqBuilder("rabbit-test", exchange, data);
        rabbitMqBuilder.setRoutingKey(routingKey);
        rabbitMqBuilder.setToJson(true);
        rabbitMqBuilder.getDescriptor().setConfigs(new Configs(rabbitConfigs));

        project.getBuildersList().add(rabbitMqBuilder);

        // LAUNCH BUILD
        System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
        FreeStyleBuild build = project.scheduleBuild2(0, new ParametersAction(parameters)).get();

        // GET OUTPUT
        String console = FileUtils.readFileToString(build.getLogFile());

        // ASSERTIONS
        Assertions.assertThat(console).containsSubsequence(
                "Retrieving parameters",
                "Initialisation Rabbit-MQ",
                "Building message",
                "Sending message",
                "Connection destroyed",
                "Finished: SUCCESS");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(RabbitMqFactory.mockRabbitTemplate).convertAndSend(Mockito.eq(exchange), Mockito.eq(routingKey), captor.capture());
        String value = captor.getValue();
        Assertions.assertThat(value).isNotNull();
        Assertions.assertThat(value).isEqualTo("{\"key1\":\"value_test\"}");
    }

    @Test
    public void test_with_build_parameter_raw() throws IOException, ExecutionException, InterruptedException {
        String exchange = "FD-exchange";
        String routingKey = "frogdevelopment.test";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Unit_Test");

        // BUILD PARAMETERS
        List<ParameterValue> parameters = new ArrayList<>();
        parameters.add(new StringParameterValue("VALUE_NAME", "value_test"));

        // RABBIT CONFIG
        ArrayList<RabbitConfig> rabbitConfigs = new ArrayList<>();
        rabbitConfigs.add(RABBIT_CONFIG);

        String data = "key_1=\"${VALUE_NAME}\"";

        RabbitMqBuilder rabbitMqBuilder = new RabbitMqBuilder("rabbit-test", exchange, data);
        rabbitMqBuilder.setRoutingKey(routingKey);
        rabbitMqBuilder.setToJson(false);
        rabbitMqBuilder.getDescriptor().setConfigs(new Configs(rabbitConfigs));

        project.getBuildersList().add(rabbitMqBuilder);

        // LAUNCH BUILD
        System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
        FreeStyleBuild build = project.scheduleBuild2(0, new ParametersAction(parameters)).get();

        // GET OUTPUT
        String console = FileUtils.readFileToString(build.getLogFile());

        // ASSERTIONS
        Assertions.assertThat(console).containsSubsequence(
                "Retrieving parameters",
                "Initialisation Rabbit-MQ",
                "Building message",
                "Sending message",
                "Connection destroyed",
                "Finished: SUCCESS");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(RabbitMqFactory.mockRabbitTemplate).convertAndSend(Mockito.eq(exchange), Mockito.eq(routingKey), captor.capture());
        String value = captor.getValue();
        Assertions.assertThat(value).isNotNull();
        Assertions.assertThat(value).isEqualTo("key_1=\"value_test\"");
    }

    @Test
    public void test_with_empty_key() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Unit_Test");

        // RABBIT CONFIG
        ArrayList<RabbitConfig> rabbitConfigs = new ArrayList<>();
        rabbitConfigs.add(RABBIT_CONFIG);

        RabbitMqBuilder rabbitMqBuilder = new RabbitMqBuilder("rabbit-test", "frogdevelopment.test", "=empty");
        rabbitMqBuilder.setRoutingKey("FD-exchange");
        rabbitMqBuilder.setToJson(true);
        rabbitMqBuilder.getDescriptor().setConfigs(new Configs(rabbitConfigs));

        project.getBuildersList().add(rabbitMqBuilder);

        // LAUNCH BUILD
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // GET OUTPUT
        String console = FileUtils.readFileToString(build.getLogFile());

        // ASSERTIONS
        Assertions.assertThat(console).containsSubsequence(
                "Retrieving parameters",
                "Initialisation Rabbit-MQ",
                "Building message",
//                "Empty key for : =empty",
                "Error while sending to Rabbit-MQ : IllegalStateException: Incorrect data",
                "Build step 'Publish to Rabbit-MQ' marked build as failure",
                "Finished: FAILURE");
    }

    @Test
    public void test_with_incorrect_format() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Unit_Test");

        // RABBIT CONFIG
        ArrayList<RabbitConfig> rabbitConfigs = new ArrayList<>();
        rabbitConfigs.add(RABBIT_CONFIG);

        RabbitMqBuilder rabbitMqBuilder = new RabbitMqBuilder("rabbit-test", "frogdevelopment.test", "incorrect:format");
        rabbitMqBuilder.setRoutingKey("FD-exchange");
        rabbitMqBuilder.setToJson(true);
        rabbitMqBuilder.getDescriptor().setConfigs(new Configs(rabbitConfigs));

        project.getBuildersList().add(rabbitMqBuilder);

        // LAUNCH BUILD
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // GET OUTPUT
        String console = FileUtils.readFileToString(build.getLogFile());

        // ASSERTIONS
        Assertions.assertThat(console).containsSubsequence(
                "Retrieving parameters",
                "Initialisation Rabbit-MQ",
                "Building message",
//                "Incorrect parameters format : incorrect:format",
                "Error while sending to Rabbit-MQ : IllegalStateException: Incorrect data",
                "Build step 'Publish to Rabbit-MQ' marked build as failure",
                "Finished: FAILURE");
    }

    @Test
    @WithoutJenkins
    public void test_RabbitConfig_fromJSON() {
        // data
        JSONObject rabbitConfigJSON = new JSONObject();
        rabbitConfigJSON.put("name", "name_value");
        rabbitConfigJSON.put("host", "host_value");
        rabbitConfigJSON.put("port", 123);
        rabbitConfigJSON.put("username", "username_value");
        rabbitConfigJSON.put("password", "password_value");

        // call
        RabbitConfig rabbitConfig = RabbitConfig.fromJSON(rabbitConfigJSON);

        // assertions
        Assertions.assertThat(rabbitConfig.getName()).isEqualTo(rabbitConfigJSON.getString("name"));
        Assertions.assertThat(rabbitConfig.getHost()).isEqualTo(rabbitConfigJSON.getString("host"));
        Assertions.assertThat(rabbitConfig.getPort()).isEqualTo(rabbitConfigJSON.getInt("port"));
        Assertions.assertThat(rabbitConfig.getUsername()).isEqualTo(rabbitConfigJSON.getString("username"));
        Assertions.assertThat(rabbitConfig.getPassword()).isEqualTo(rabbitConfigJSON.getString("password"));
    }

    @Test
    @WithoutJenkins
    public void test_Configs_fromJSON_0_config() {
        // call
        Configs configs = Configs.fromJSON(new JSONObject());

        // assertions
        Assertions.assertThat(configs).isNull();
    }

    @Test
    @WithoutJenkins
    public void test_Configs_fromJSON_1_config() {
        // data
        JSONObject rabbitConfigJSON = new JSONObject();
        rabbitConfigJSON.put("name", "name_value");
        rabbitConfigJSON.put("host", "host_value");
        rabbitConfigJSON.put("port", 123);
        rabbitConfigJSON.put("username", "username_value");
        rabbitConfigJSON.put("password", "password_value");


        JSONObject configsJSON = new JSONObject();
        configsJSON.put("rabbitConfigs", rabbitConfigJSON);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("configs", configsJSON);

        // call
        Configs configs = Configs.fromJSON(jsonObject);

        // assertions
        Assertions.assertThat(configs).isNotNull();
        Assertions.assertThat(configs.getRabbitConfigs()).hasSize(1);
        RabbitConfig rabbitConfig = configs.getRabbitConfigs().get(0);
        Assertions.assertThat(rabbitConfig.getName()).isEqualTo(rabbitConfigJSON.getString("name"));
        Assertions.assertThat(rabbitConfig.getHost()).isEqualTo(rabbitConfigJSON.getString("host"));
        Assertions.assertThat(rabbitConfig.getPort()).isEqualTo(rabbitConfigJSON.getInt("port"));
        Assertions.assertThat(rabbitConfig.getUsername()).isEqualTo(rabbitConfigJSON.getString("username"));
        Assertions.assertThat(rabbitConfig.getPassword()).isEqualTo(rabbitConfigJSON.getString("password"));
    }

    @Test
    @WithoutJenkins
    public void test_Configs_fromJSON_n_configs() {
        // data
        JSONObject rabbitConfigJSON_1 = new JSONObject();
        rabbitConfigJSON_1.put("name", "name_value1");
        rabbitConfigJSON_1.put("host", "host_value1");
        rabbitConfigJSON_1.put("port", 123);
        rabbitConfigJSON_1.put("username", "username_value1");
        rabbitConfigJSON_1.put("password", "password_value1");

        JSONObject rabbitConfigJSON_2 = new JSONObject();
        rabbitConfigJSON_2.put("name", "name_value2");
        rabbitConfigJSON_2.put("host", "host_value2");
        rabbitConfigJSON_2.put("port", 456);
        rabbitConfigJSON_2.put("username", "username_value2");
        rabbitConfigJSON_2.put("password", "password_value2");

        JSONArray rabbitConfigsJSONArray = new JSONArray();
        rabbitConfigsJSONArray.add(rabbitConfigJSON_1);
        rabbitConfigsJSONArray.add(rabbitConfigJSON_2);

        JSONObject configsJSON = new JSONObject();
        configsJSON.put("rabbitConfigs", rabbitConfigsJSONArray);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("configs", configsJSON);

        // call
        Configs configs = Configs.fromJSON(jsonObject);

        // assertions
        Assertions.assertThat(configs).isNotNull();
        Assertions.assertThat(configs.getRabbitConfigs()).hasSize(2);
        RabbitConfig rabbitConfig_1 = configs.getRabbitConfigs().get(0);
        Assertions.assertThat(rabbitConfig_1.getName()).isEqualTo(rabbitConfigJSON_1.getString("name"));
        Assertions.assertThat(rabbitConfig_1.getHost()).isEqualTo(rabbitConfigJSON_1.getString("host"));
        Assertions.assertThat(rabbitConfig_1.getPort()).isEqualTo(rabbitConfigJSON_1.getInt("port"));
        Assertions.assertThat(rabbitConfig_1.getUsername()).isEqualTo(rabbitConfigJSON_1.getString("username"));
        Assertions.assertThat(rabbitConfig_1.getPassword()).isEqualTo(rabbitConfigJSON_1.getString("password"));

        RabbitConfig rabbitConfig_2 = configs.getRabbitConfigs().get(1);
        Assertions.assertThat(rabbitConfig_2.getName()).isEqualTo(rabbitConfigJSON_2.getString("name"));
        Assertions.assertThat(rabbitConfig_2.getHost()).isEqualTo(rabbitConfigJSON_2.getString("host"));
        Assertions.assertThat(rabbitConfig_2.getPort()).isEqualTo(rabbitConfigJSON_2.getInt("port"));
        Assertions.assertThat(rabbitConfig_2.getUsername()).isEqualTo(rabbitConfigJSON_2.getString("username"));
        Assertions.assertThat(rabbitConfig_2.getPassword()).isEqualTo(rabbitConfigJSON_2.getString("password"));
    }


    @Test
    @WithoutJenkins
    public void test_RabbitConfigDescriptor_doTestConnection_isOpen_true() throws IOException, TimeoutException {
        // data
        RabbitConfigDescriptor rabbitConfigDescriptor = new RabbitConfigDescriptor();
        String host = "host";
        String port = "5667";
        String username = "username";
        String password = "password";

        // create mock
        ConnectionFactory mockConnectionFactory = RabbitMqFactory.createConnectionFactory(
                username,
                host,
                host,
                Integer.parseInt(port)
        );
        Connection connection = Mockito.mock(Connection.class);

        // mock
        Mockito.doReturn(connection).when(mockConnectionFactory).newConnection();
        Mockito.doReturn(true).when(connection).isOpen();

        // call
        FormValidation formValidation = rabbitConfigDescriptor.doTestConnection(host, port, username, password);

        // assertions
        Assertions.assertThat(formValidation.kind).isEqualTo(FormValidation.Kind.OK);
        Assertions.assertThat(formValidation.getMessage()).isEqualTo("Connection success");
    }

    @Test
    @WithoutJenkins
    public void test_RabbitConfigDescriptor_doTestConnection_isOpen_false() throws IOException, TimeoutException {
        // data
        RabbitConfigDescriptor rabbitConfigDescriptor = new RabbitConfigDescriptor();
        String host = "host";
        String port = "5667";
        String username = "username";
        String password = "password";

        // create mock
        ConnectionFactory mockConnectionFactory = RabbitMqFactory.createConnectionFactory(
                username,
                host,
                host,
                Integer.parseInt(port)
        );
        Connection connection = Mockito.mock(Connection.class);

        // mock
        Mockito.doReturn(connection).when(mockConnectionFactory).newConnection();
        Mockito.doReturn(false).when(connection).isOpen();

        // call
        FormValidation formValidation = rabbitConfigDescriptor.doTestConnection(host, port, username, password);

        // assertions
        Assertions.assertThat(formValidation.kind).isEqualTo(FormValidation.Kind.ERROR);
        Assertions.assertThat(formValidation.getMessage()).isEqualTo("Connection failed");
    }

    @Test
    @WithoutJenkins
    public void test_RabbitConfigDescriptor_doTestConnection_exception() throws IOException, TimeoutException {
        // data
        RabbitConfigDescriptor rabbitConfigDescriptor = new RabbitConfigDescriptor();
        String host = "host";
        String port = "5667";
        String username = "username";
        String password = "password";

        IOException exception_for_text = new IOException("Mocked exception for text");

        // create mock
        ConnectionFactory mockConnectionFactory = RabbitMqFactory.createConnectionFactory(
                username,
                host,
                host,
                Integer.parseInt(port)
        );

        // mock
        Mockito.doThrow(exception_for_text).when(mockConnectionFactory).newConnection();

        // call
        FormValidation formValidation = rabbitConfigDescriptor.doTestConnection(host, port, username, password);

        // assertions
        Assertions.assertThat(formValidation.kind).isEqualTo(FormValidation.Kind.ERROR);
        Assertions.assertThat(formValidation.getMessage()).isEqualTo("Client error : " + exception_for_text.getMessage());
    }

}