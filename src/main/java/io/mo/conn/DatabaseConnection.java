package io.mo.conn;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnection {
    Connection BuildDatabaseConnection();
}
