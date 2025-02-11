package io.kestra.plugin.aws.dynamodb;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Collections;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(
    examples = {
        @Example(
            title = "Put an item from a map.",
            code = {
                "tableName: \"persons\"",
                "item:",
                "  id: 1",
                "  firstname: \"John\"",
                "  lastname: \"Doe\"",
            }
        ),
        @Example(
            title = "Put an item from a JSON string.",
            code = {
                "tableName: \"persons\"",
                "item: \"{{ outputs.task_id.data | json }}\""
            }
        )
    }
)
@Schema(
    title = "Put an item into a DynamoDB table, if it already exist the element will be updated."
)
public class PutItem extends AbstractDynamoDb implements RunnableTask<VoidOutput> {
    @Schema(
        title = "The DynamoDB item.",
        description = "Can be a JSON string, or a map."
    )
    @PluginProperty(dynamic = true)
    private Object item;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        try (var dynamoDb = client(runContext)) {
            var fields = fields(runContext, this.item);
            var item = valueMapFrom(fields);

            var putRequest = PutItemRequest.builder()
                .tableName(runContext.render(this.tableName))
                .item(item)
                .build();
            dynamoDb.putItem(putRequest);

            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fields(RunContext runContext, Object value) throws IllegalVariableEvaluationException, JsonProcessingException {
        if (value instanceof String) {
            return JacksonMapper.toMap(runContext.render((String) value));
        } else if (value instanceof Map) {
            return runContext.render((Map<String, Object>) value);
        } else if (value == null) {
            return Collections.emptyMap();
        }

        throw new IllegalVariableEvaluationException("Invalid value type '" + value.getClass() + "'");
    }

}
