/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.starrocks.connector.flink;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.junit.Test;

import mockit.Expectations;

import static org.junit.Assert.assertFalse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StarRocksDynamicTableSinkITTest extends StarRocksSinkBaseTest {
    
    @Test
    public void testBatchSink() {
        List<Map<String, String>> meta = new ArrayList<>();
        meta.add(new HashMap<String, String>(){{
            put("COLUMN_NAME", "name");
            put("COLUMN_KEY", "");
            put("DATA_TYPE", "varchar");
        }});
        meta.add(new HashMap<String, String>(){{
            put("COLUMN_NAME", "score");
            put("COLUMN_KEY", "");
            put("DATA_TYPE", "bigint");
        }});
        meta.add(new HashMap<String, String>(){{
            put("COLUMN_NAME", "d");
            put("COLUMN_KEY", "");
            put("DATA_TYPE", "date");
        }});
        meta.add(new HashMap<String, String>(){{
            put("COLUMN_NAME", "t");
            put("COLUMN_KEY", "");
            put("DATA_TYPE", "datetime");
        }});
        new Expectations(){
            {
                v.getTableColumnsMetaData();
                result = meta;
            }
        };
        EnvironmentSettings bsSettings = EnvironmentSettings.newInstance()
            .useBlinkPlanner().inBatchMode().build();
        TableEnvironment tEnv = TableEnvironment.create(bsSettings);
        mockSuccessResponse();
        String createSQL = "CREATE TABLE USER_RESULT(" +
            "name VARCHAR," +
            "score BIGINT," +
            "t TIMESTAMP(3)," +
            "d DATE" +
            ") WITH ( " +
            "'connector' = 'starrocks'," +
            "'jdbc-url'='" + OPTIONS.getJdbcUrl() + "'," +
            "'load-url'='" + String.join(";", OPTIONS.getLoadUrlList()) + "'," +
            "'database-name' = '" + OPTIONS.getDatabaseName() + "'," +
            "'table-name' = '" + OPTIONS.getTableName() + "'," +
            "'username' = '" + OPTIONS.getUsername() + "'," +
            "'password' = '" + OPTIONS.getPassword() + "'," +
            "'sink.buffer-flush.max-rows' = '" + OPTIONS.getSinkMaxRows() + "'," +
            "'sink.buffer-flush.max-bytes' = '" + OPTIONS.getSinkMaxBytes() + "'," +
            "'sink.buffer-flush.interval-ms' = '" + OPTIONS.getSinkMaxFlushInterval() + "'," +
            "'sink.properties.column_separator' = '\\x01'," +
            "'sink.properties.row_delimiter' = '\\x02'" +
            ")";
        tEnv.executeSql(createSQL);

        String exMsg = "";
        try {
            tEnv.executeSql("INSERT INTO USER_RESULT\n" +
                "VALUES ('lebron', 99, TO_TIMESTAMP('2020-01-01 01:00:01'), TO_DATE('2020-01-01'))").collect();
            tEnv.executeSql("INSERT INTO USER_RESULT\n" +
                "VALUES ('lebron', 99, TO_TIMESTAMP('2020-01-01 12:00:01'), TO_DATE('2020-01-01')), ('stephen', 99, TO_TIMESTAMP('2020-01-01 23:00:01'), TO_DATE('2020-01-01'))").collect();
            Thread.sleep(2000);
        } catch (Exception e) {
            exMsg = e.getMessage();
        }
        assertFalse(exMsg, exMsg.length() > 0);
    }
}