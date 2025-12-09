package mci.dibse.crazylabyrinth.client.domain.model.enums;

public enum CommandTypes {
    CONNECT("connect");

    final String command;

    CommandTypes(String command) {
        this.command = command;
    }
    public String getCommand() {
        return command;
    }

}
