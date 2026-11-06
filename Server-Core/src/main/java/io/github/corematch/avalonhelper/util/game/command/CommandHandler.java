/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.game.command;

import io.github.corematch.avalonhelper.func.ConsSeq;
import io.github.corematch.avalonhelper.struct.map.ObjectMap;
import io.github.corematch.avalonhelper.struct.list.Seq;
import io.github.corematch.avalonhelper.util.log.Log;

/**
 * @author Dr (dr@der.kim)
 */
@SuppressWarnings("unchecked")
public class CommandHandler {
    private final ObjectMap<String, Command> commands = new ObjectMap<>(16);
    private final Seq<Command> orderedCommands = new Seq<>();
    private String prefix;

    /**
     * Creates a command handler with a specific command prefix.
     */
    public CommandHandler(String prefix) {
        this.prefix = prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Handles a message with no additional parameters.
     */
    public CommandResponse handleMessage(String message) {
        return handleMessage(message, null);
    }

    public CommandResponse handleMessage(String message, Object params) {
        if (message == null || (!message.startsWith(prefix))) {
            return new CommandResponse(ResponseType.noCommand, null, null);
        }

        message = message.substring(prefix.length());

        String commandstr = message.contains(" ") ? message.substring(0, message.indexOf(" ")) : message;
        String argstr = message.contains(" ") ? message.substring(commandstr.length() + 1) : "";

        Seq<String> result = new Seq<>();

        Command command = commands.get(commandstr.toLowerCase());

        if (command != null) {
            int index = 0;
            boolean satisfied = false;

            while (true) {
                if (index >= command.params.length && !argstr.isEmpty()) {
                    return new CommandResponse(ResponseType.manyArguments, command, commandstr);
                } else if (argstr.isEmpty()) {
                    break;
                }

                if (command.params[index].optional || index >= command.params.length - 1 || command.params[index + 1].optional) {
                    satisfied = true;
                }

                if (command.params[index].variadic) {
                    result.add(argstr);
                    break;
                }

                int next = argstr.indexOf(" ");
                if (next == -1) {
                    if (!satisfied) {
                        return new CommandResponse(ResponseType.fewArguments, command, commandstr);
                    }
                    result.add(argstr);
                    break;
                } else {
                    String arg = argstr.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)")[0];
                    argstr = argstr.substring(arg.length() + 1);
                    result.add(arg.replace("\"", ""));
                }
                index++;
            }

            if (!satisfied && command.params.length > 0 && !command.params[0].optional) {
                return new CommandResponse(ResponseType.fewArguments, command, commandstr);
            }

            command.runner.accept(result.toArray(String.class), params);

            return new CommandResponse(ResponseType.valid, command, commandstr);
        } else {
            return new CommandResponse(ResponseType.unknownCommand, null, commandstr);
        }
    }

    public void removeCommand(String text) {
        Command c = commands.get(text);
        if (c == null) {
            return;
        }
        commands.remove(text);
        orderedCommands.remove(c);
    }

    /**
     * Register a command which handles a zero-sized list of arguments and one parameter.
     */
    public <T> Command register(String text, String description, CommandRunner<T> runner) {
        Command cmd = new Command(text, "", description, runner);
        commands.put(text.toLowerCase(), cmd);
        orderedCommands.add(cmd);
        return cmd;
    }

    public <T> Command register(String text, String params, String description, CommandRunner<T> runner) {
        Command cmd = new Command(text, params, description, runner);
        commands.put(text.toLowerCase(), cmd);
        orderedCommands.add(cmd);
        return cmd;
    }

    public Command register(String text, String description, ConsSeq<String[]> runner) {
        return register(text, description, (args, p) -> runner.invoke(args));
    }

    public Command register(String text, String params, String description, ConsSeq<String[]> runner) {
        return register(text, params, description, (args, p) -> runner.invoke(args));
    }

    public Seq<Command> getCommandList() {
        return orderedCommands;
    }

    public enum ResponseType {
        /**
         * 命令状态
         */
        noCommand, unknownCommand, fewArguments, manyArguments, valid
    }

    public static class Command {
        public final String text;
        public final String paramText;
        public final String description;
        public final CommandParam[] params;
        private final CommandRunner runner;

        public Command(String text, String paramText, String description, CommandRunner runner) {
            this.text = text;
            this.paramText = paramText;
            this.runner = runner;
            this.description = description;

            String[] psplit = paramText.split(" ");
            if (paramText.isEmpty()) {
                params = new CommandParam[0];
            } else {
                params = new CommandParam[psplit.length];

                boolean hadOptional = false;

                for (int i = 0; i < params.length; i++) {
                    String param = psplit[i];

                    if (param.length() <= 2) {
                        throw new IllegalArgumentException("Malformed param '" + param + "'");
                    }

                    char l = param.charAt(0), r = param.charAt(param.length() - 1);
                    boolean optional, variadic = false;

                    if (l == '<' && r == '>') {
                        if (hadOptional) {
                            throw new IllegalArgumentException("Can't have non-optional param after optional param!");
                        }
                        optional = false;
                    } else if (l == '[' && r == ']') {
                        optional = true;
                    } else {
                        throw new IllegalArgumentException("Malformed param '" + param + "'");
                    }

                    if (optional) {
                        hadOptional = true;
                    }

                    String fname = param.substring(1, param.length() - 1);
                    if (fname.endsWith("...")) {
                        if (i != params.length - 1) {
                            throw new IllegalArgumentException("A variadic parameter should be the last parameter!");
                        }

                        fname = fname.substring(0, fname.length() - 3);
                        variadic = true;
                    }

                    params[i] = new CommandParam(fname, optional, variadic);

                }
            }
        }
    }

    public interface CommandRunner<T> {
        /**
         * 接收参数
         *
         * @param args      命令分片组
         * @param parameter Run
         */
        void accept(String[] args, T parameter);
    }

    public static class CommandParam {
        public final String name;
        public final boolean optional;
        public final boolean variadic;

        public CommandParam(String name, boolean optional, boolean variadic) {
            this.name = name;
            this.optional = optional;
            this.variadic = variadic;
        }
    }

    public static class CommandResponse {
        public final ResponseType type;
        public final Command command;
        public final String runCommand;

        public CommandResponse(ResponseType type, Command command, String runCommand) {
            this.type = type;
            this.command = command;
            this.runCommand = runCommand;
        }
    }
}
