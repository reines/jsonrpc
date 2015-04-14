package com.jamierf.jsonrpc.codec.fastjson.deserializers;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.google.common.base.Optional;
import com.jamierf.jsonrpc.api.ErrorMessage;

import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkArgument;

public class ErrorMessageDeserializer implements ObjectDeserializer {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialze(final DefaultJSONParser parser, final Type type, final Object fieldName) {
        final JSONLexer lexer = parser.getLexer();

        if (lexer.token() == JSONToken.NULL) {
            lexer.nextToken();
            return null;
        }

        parser.accept(JSONToken.LBRACE);

        int code = 0;
        String message = null;

        while (true) {
            final String key = lexer.stringVal();
            lexer.nextToken(JSONToken.COLON);
            parser.accept(JSONToken.COLON);

            switch (key) {
                case "message":
                    if (lexer.token() != JSONToken.LITERAL_STRING) {
                        throw new JSONException("message is not string");
                    }
                    message = lexer.stringVal();
                    lexer.nextToken();
                    break;
                case "code":
                    if (lexer.token() != JSONToken.LITERAL_INT) {
                        throw new JSONException("code is not int");
                    }
                    code = lexer.intValue();
                    lexer.nextToken();
                    break;
                default:
                    parser.parse();
                    break;
            }

            if (lexer.token() == JSONToken.COMMA) {
                lexer.nextToken();
                continue;
            }

            break;
        }

        parser.accept(JSONToken.RBRACE);

        checkArgument(code != 0, "Code not present");
        checkArgument(message != null, "Message not present");

        return (T) new ErrorMessage<>(code, message, Optional.absent());
    }

    @Override
    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }
}
