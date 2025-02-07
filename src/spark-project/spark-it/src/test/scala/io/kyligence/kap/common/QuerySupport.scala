/*
 * Copyright (C) 2016 Kyligence Inc. All rights reserved.
 * http://kyligence.io
 * This software is the confidential and proprietary information of
 * Kyligence Inc. ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with
 * Kyligence Inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package io.kyligence.kap.common

import java.util.Locale

import org.apache.commons.lang3.StringUtils
import org.apache.kylin.common.KylinConfig
import org.apache.kylin.common.util.Unsafe
import org.apache.kylin.metadata.project.NProjectManager
import org.apache.kylin.query.engine.QueryExec
import org.apache.kylin.query.engine.data.QueryResult
import org.apache.kylin.query.util.{QueryParams, QueryUtil}
import org.apache.kylin.util.ExecAndComp.EnhancedQueryResult
import org.apache.kylin.util.{ExecAndComp, QueryResultComparator}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.common.SparderQueryTest
import org.apache.spark.sql.{DataFrame, SparderEnv}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import io.netty.util.internal.ThrowableUtil

trait QuerySupport
  extends BeforeAndAfterAll
    with BeforeAndAfterEach
    with Logging {
  self: Suite =>
  val sparder = System.getProperty("kylin.query.engine.sparder-enabled")


  override def beforeAll(): Unit = {
    super.beforeAll()
    Unsafe.setProperty("kylin.query.engine.sparder-enabled", "true")
  }

  override def afterAll(): Unit = {
    super.afterAll()
    if (sparder != null) {
      Unsafe.setProperty("kylin.query.engine.sparder-enabled", sparder)
    } else {
      Unsafe.clearProperty("kylin.query.engine.sparder-enabled")
    }
  }

  def singleQuery(sql: String, project: String): DataFrame = {
    val prevRunLocalConf = Unsafe.setProperty("kylin.query.engine.run-constant-query-locally", "FALSE")
    try {
      val queryExec = new QueryExec(project, KylinConfig.getInstanceFromEnv)
      val queryParams = new QueryParams(NProjectManager.getProjectConfig(project), sql, project,
        0, 0, queryExec.getDefaultSchemaName, true)
      val convertedSql = QueryUtil.massageSql(queryParams)
      queryExec.executeQuery(convertedSql)
    } finally {
      if (prevRunLocalConf == null) {
        Unsafe.clearProperty("kylin.query.engine.run-constant-query-locally")
      } else {
        Unsafe.setProperty("kylin.query.engine.run-constant-query-locally", prevRunLocalConf)
      }
    }
    SparderEnv.getDF
  }

  def changeJoinType(sql: String, targetType: String): String = {
    if (targetType.equalsIgnoreCase("default")) return sql
    val specialStr = "changeJoinType_DELIMITERS"
    val replaceSql = sql.replaceAll(System.lineSeparator(), " " + specialStr + " ")
    val tokens = StringUtils.split(replaceSql, null)
    // split white spaces
    var i = 0
    while (i < tokens.length - 1) {
      if ((tokens(i).equalsIgnoreCase("inner") || tokens(i).equalsIgnoreCase(
        "left")) &&
        tokens(i + 1).equalsIgnoreCase("join")) {
        tokens(i) = targetType.toLowerCase(Locale.ROOT)
      }
      i += 1
    }
    var ret = tokens.mkString(" ")
    ret = ret.replaceAll(specialStr, System.lineSeparator())
    ret
  }

  def checkWithSparkSql(sqlText: String, project: String): String = {
    val df = SparderEnv.getSparkSession.sql(sqlText)
    df.show(1000)
    SparderQueryTest.checkAnswer(df, singleQuery(sqlText, project))
  }

  def runAndCompare(querySql: String,
                    project: String,
                    joinType: String,
                    filename: String,
                    checkOrder: Boolean,
                    sparkSql: Option[String] = None,
                    extraComparator: (EnhancedQueryResult, QueryResult) => Boolean = (_, _) => true): String = {
    try {
      val modelResult = ExecAndComp.queryModelWithOlapContext(project, joinType, querySql)

      var startTs = System.currentTimeMillis
      val normalizedSql = ExecAndComp.removeDataBaseInSql(sparkSql.getOrElse(querySql))
      val sparkResult = ExecAndComp.queryWithSpark(project, normalizedSql, joinType, filename)
      log.info("Query with Spark Duration(ms): {}", System.currentTimeMillis - startTs)

      startTs = System.currentTimeMillis
      var result = QueryResultComparator.compareResults(sparkResult, modelResult.getQueryResult,
        if (checkOrder) ExecAndComp.CompareLevel.SAME_ORDER else ExecAndComp.CompareLevel.SAME)
      result = result && extraComparator.apply(modelResult, sparkResult)
      log.info("Compare Duration(ms): {}", System.currentTimeMillis - startTs)

      if (!result) {
        val queryErrorMsg = s"$joinType\n$filename\n $querySql\n"
        if ("true".equals(System.getProperty("Failfast"))) {
          throw new RuntimeException(queryErrorMsg)
        }
        queryErrorMsg
      } else {
        null
      }
    } catch {
      case exception: Exception =>
        if ("true".equals(System.getProperty("Failfast"))) {
          throw exception
        } else {
          s"$joinType\n$filename\n $querySql\n" + ThrowableUtil.stackTraceToString(exception)
        }
    }
  }
}
