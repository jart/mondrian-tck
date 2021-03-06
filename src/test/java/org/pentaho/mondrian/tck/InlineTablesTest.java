/*******************************************************************************
 *
 * Pentaho Mondrian Test Compatibility Kit
 *
 * Copyright (C) 2013-2014 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.pentaho.mondrian.tck;

import static org.pentaho.mondrian.tck.SqlExpectation.newBuilder;
import org.junit.Test;

public class InlineTablesTest extends TestBase {
  @Test
  public void testInlineTable() throws Exception {
    final SqlExpectation expct =
        newBuilder()
          .query( "select\n"
            + "    alt_promotion.promo_id promo_id,\n"
            + "    alt_promotion.promo_name promo_name\n"
            + "from\n"
            + "    (select 0 promo_id, 'Promo0' promo_name union all select 1 promo_id, 'Promo1' promo_name) alt_promotion\n"
            + "group by\n"
            + "    alt_promotion.promo_id,\n"
            + "    alt_promotion.promo_name\n"
            + "order by\n"
            + "    " + getOrderExpression( "c0", "alt_promotion.promo_id", true, true, true ) )
          .columns( "promo_id", "promo_name" )
          .rows( "0|Promo0" )
          .partial()
          .build();
    SqlContext.defaultContext().verify( expct );
  }

  @Test
  public void testInlineTableJoin() throws Exception {
    final SqlExpectation expct =
        newBuilder()
          .query( "select\n"
              + "    time_by_day.the_year the_year,\n"
              + "    nation.nation_name nation_name,\n"
              + "    sum(sales_fact_1997.unit_sales) sum_sales\n"
              + "from\n"
              + "    time_by_day time_by_day,\n"
              + "    sales_fact_1997 sales_fact_1997,\n"
              + "    (select 'USA' nation_name, 'US' nation_shortcode union all select 'Mexico' nation_name, 'MX' nation_shortcode union all select 'Canada' nation_name, 'CA' nation_shortcode) nation,\n"
              + "    store store\n"
              + "where\n"
              + "    sales_fact_1997.time_id = time_by_day.time_id\n"
              + "and\n"
              + "    time_by_day.the_year = 1997\n"
              + "and\n"
              + "    sales_fact_1997.store_id = store.store_id\n"
              + "and\n"
              + "    store.store_country = nation.nation_name\n"
              + "group by\n"
              + "    time_by_day.the_year,\n"
              + "    nation.nation_name" )
          .columns( "the_year", "nation_name", "sum_sales" )
          .rows( "1,997|USA|266,773" )
          .partial()
          .build();
    SqlContext.defaultContext().verify( expct );
  }

  @Test
  public void testInlineTableMondrian() throws Exception {
    MondrianExpectation expectation = MondrianExpectation.newBuilder()
        .query( "select "
                + "  {[Alternative Promotion].[All Alternative Promotions].children} ON COLUMNS "
                + "  from Sales_inline" )
        .result( "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Alternative Promotion].[Promo0]}\n"
                + "{[Alternative Promotion].[Promo1]}\n"
                + "Row #0: 195,448\n"
                + "Row #0: \n" )
        .sql( "select\n"
          + "    alt_promotion.promo_id c0,\n"
          + "    alt_promotion.promo_name c1\n"
          + "from\n"
          + "    (select 0 promo_id, 'Promo0' promo_name union all select 1 promo_id, 'Promo1' promo_name) alt_promotion\n"
          + "group by\n"
          + "    alt_promotion.promo_id,\n"
          + "    alt_promotion.promo_name\n"
          + "order by\n"
          + "    " + getOrderExpression( "c0", "alt_promotion.promo_id", true, true, true ) )
        .sql( "select count(*) from (select distinct\n"
          + "    alt_promotion.promo_id c0\n"
          + "from\n"
          + "    (select 0 promo_id, 'Promo0' promo_name union all select 1 promo_id, 'Promo1' promo_name) alt_promotion)"
          + ( dialect.requiresAliasForFromQuery()
            ? " init"
            : "" ) )
        .sql( "select\n"
          + "    alt_promotion.promo_id c0,\n"
          + "    sum(sales_fact_1997.unit_sales) m0\n"
          + "from\n"
          + "    (select 0 promo_id, 'Promo0' promo_name union all select 1 promo_id, 'Promo1' promo_name) alt_promotion,\n"
          + "    sales_fact_1997 sales_fact_1997\n"
          + "where\n"
          + "    sales_fact_1997.promotion_id = alt_promotion.promo_id\n"
          + "group by\n"
          + "    alt_promotion.promo_id" )
        .build();
    MondrianContext.forCatalog(
        "<Schema name=\"FoodMart\">"
        + "<Cube name=\"Sales_inline\">\n"
        + "  <Table name=\"sales_fact_1997\"/>\n"
        + "  <Dimension name=\"Alternative Promotion\" foreignKey=\"promotion_id\">\n"
        + "    <Hierarchy hasAll=\"true\" primaryKey=\"promo_id\">\n"
        + "      <InlineTable alias=\"alt_promotion\">\n"
        + "        <ColumnDefs>\n"
        + "          <ColumnDef name=\"promo_id\" type=\"Numeric\"/>\n"
        + "          <ColumnDef name=\"promo_name\" type=\"String\"/>\n"
        + "        </ColumnDefs>\n"
        + "        <Rows>\n"
        + "          <Row>\n"
        + "            <Value column=\"promo_id\">0</Value>\n"
        + "            <Value column=\"promo_name\">Promo0</Value>\n"
        + "          </Row>\n"
        + "          <Row>\n"
        + "            <Value column=\"promo_id\">1</Value>\n"
        + "            <Value column=\"promo_name\">Promo1</Value>\n"
        + "          </Row>\n"
        + "        </Rows>\n"
        + "      </InlineTable>\n"
        + "      <Level name=\"Alternative Promotion\" column=\"promo_id\" nameColumn=\"promo_name\" uniqueMembers=\"true\"/> \n"
        + "    </Hierarchy>\n"
        + "  </Dimension>\n"
        + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
        + "      formatString=\"Standard\" visible=\"false\"/>\n"
        + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
        + "      formatString=\"#,###.00\"/>\n"
        + "</Cube>"
        + "</Schema>" ).verify( expectation );
  }

  @Test
  public void testInlineTableInSharedDim() throws Exception {
    MondrianExpectation expectation =
        MondrianExpectation.newBuilder()
          .query( "select "
            + "  {[Shared Alternative Promotion].[All Shared Alternative Promotions].children} ON COLUMNS "
            + "  from Sales_inline_shared" )
          .result( "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Shared Alternative Promotion].[First promo]}\n"
            + "{[Shared Alternative Promotion].[Second promo]}\n"
            + "Row #0: 195,448\n"
            + "Row #0: \n" )
          .sql( "select\n"
            + "    alt_promotion.promo_id c0,\n"
            + "    alt_promotion.promo_name c1\n"
            + "from\n"
            + "    (select 0 promo_id, 'First promo' promo_name union all select 1 promo_id, 'Second promo' promo_name) alt_promotion\n"
            + "group by\n"
            + "    alt_promotion.promo_id,\n"
            + "    alt_promotion.promo_name\n"
            + "order by\n"
            + "    " + getOrderExpression( "c0", "alt_promotion.promo_id", true, true, true ) )
          .sql( "select count(*) from (select distinct\n"
              + "    alt_promotion.promo_id c0\n"
              + "from\n"
              + "    (select 0 promo_id, 'First promo' promo_name union all select 1 promo_id, 'Second promo' promo_name) alt_promotion)"
              + ( dialect.requiresAliasForFromQuery()
                ? " init"
                : "" ) )
          .sql( "select\n"
              + "    alt_promotion.promo_id c0,\n"
              + "    sum(sales_fact_1997.unit_sales) m0\n"
              + "from\n"
              + "    (select 0 promo_id, 'First promo' promo_name union all select 1 promo_id, 'Second promo' promo_name) alt_promotion,\n"
              + "    sales_fact_1997 sales_fact_1997\n"
              + "where\n"
              + "    sales_fact_1997.promotion_id = alt_promotion.promo_id\n"
              + "group by\n"
              + "    alt_promotion.promo_id" )
          .build();
    MondrianContext.forCatalog(
      "<Schema name=\"FoodMart\">"
      + "  <Dimension name=\"Shared Alternative Promotion\">\n"
      + "    <Hierarchy hasAll=\"true\" primaryKey=\"promo_id\">\n"
      + "      <InlineTable alias=\"alt_promotion\">\n"
      + "        <ColumnDefs>\n"
      + "          <ColumnDef name=\"promo_id\" type=\"Numeric\"/>\n"
      + "          <ColumnDef name=\"promo_name\" type=\"String\"/>\n"
      + "        </ColumnDefs>\n"
      + "        <Rows>\n"
      + "          <Row>\n"
      + "            <Value column=\"promo_id\">0</Value>\n"
      + "            <Value column=\"promo_name\">First promo</Value>\n"
      + "          </Row>\n"
      + "          <Row>\n"
      + "            <Value column=\"promo_id\">1</Value>\n"
      + "            <Value column=\"promo_name\">Second promo</Value>\n"
      + "          </Row>\n"
      + "        </Rows>\n"
      + "      </InlineTable>\n"
      + "      <Level name=\"Alternative Promotion\" column=\"promo_id\" nameColumn=\"promo_name\" uniqueMembers=\"true\"/> \n"
      + "    </Hierarchy>\n"
      + "  </Dimension>\n"
      + "<Cube name=\"Sales_inline_shared\">\n"
      + "  <Table name=\"sales_fact_1997\"/>\n"
      + "  <DimensionUsage name=\"Shared Alternative Promotion\" source=\"Shared Alternative Promotion\" foreignKey=\"promotion_id\"/>\n"
      + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
      + "      formatString=\"Standard\" visible=\"false\"/>\n"
      + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
      + "      formatString=\"#,###.00\"/>\n"
      + "</Cube>"
      + "</Schema>" ).verify( expectation );
  }

  @Test
  public void testInlineTableSnowflake() throws Exception {
    MondrianExpectation expectation = MondrianExpectation.newBuilder()
        .query( "select "
          + "  {[Store].children} ON COLUMNS "
          + "  from Sales_inline_snowflake" )
        .result( "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Store].[CA]}\n"
          + "{[Store].[MX]}\n"
          + "{[Store].[US]}\n"
          + "Row #0: \n"
          + "Row #0: \n"
          + "Row #0: 266,773\n" )
        .sql( "select\n"
          + "    nation.nation_name c0,\n"
          + "    nation.nation_shortcode c1\n"
          + "from\n"
          + "    store store,\n"
          + "    (select 'USA' nation_name, 'US' nation_shortcode union all select 'Mexico' nation_name, 'MX' nation_shortcode union all select 'Canada' nation_name, 'CA' nation_shortcode) nation\n"
          + "where\n"
          + "    store.store_country = nation.nation_name\n"
          + "group by\n"
          + "    nation.nation_name,\n"
          + "    nation.nation_shortcode\n"
          + "order by\n"
          + "    " + getOrderExpression( "c0", "nation.nation_name", true, true, true ) )
        .sql( "select count(distinct the_year) from time_by_day" )
        .sql( "select count(*) from (select distinct\n"
          + "    nation.nation_name c0\n"
          + "from\n"
          + "    (select 'USA' nation_name, 'US' nation_shortcode union all select 'Mexico' nation_name, 'MX' nation_shortcode union all select 'Canada' nation_name, 'CA' nation_shortcode) nation)"
          + ( dialect.requiresAliasForFromQuery()
            ? " init"
            : "" ) )
        .sql( "select\n"
          + "    time_by_day.the_year c0,\n"
          + "    nation.nation_name c1,\n"
          + "    sum(sales_fact_1997.unit_sales) m0\n"
          + "from\n"
          + "    time_by_day time_by_day,\n"
          + "    sales_fact_1997 sales_fact_1997,\n"
          + "    (select 'USA' nation_name, 'US' nation_shortcode union all select 'Mexico' nation_name, 'MX' nation_shortcode union all select 'Canada' nation_name, 'CA' nation_shortcode) nation,\n"
          + "    store store\n"
          + "where\n"
          + "    sales_fact_1997.time_id = time_by_day.time_id\n"
          + "and\n"
          + "    time_by_day.the_year = 1997\n"
          + "and\n"
          + "    sales_fact_1997.store_id = store.store_id\n"
          + "and\n"
          + "    store.store_country = nation.nation_name\n"
          + "group by\n"
          + "    time_by_day.the_year,\n"
          + "    nation.nation_name" )
        .build();
    MondrianContext.forCatalog(
      "<Schema name=\"FoodMart\">"
      + "<Cube name=\"" + "Sales_inline_snowflake" + "\">\n"
      + "  <Table name=\"sales_fact_1997\"/>\n"
      + "  <Dimension name=\"Time\" foreignKey=\"time_id\">\n"
      + "   <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
      + "    <Table name=\"time_by_day\"/>\n"
      + "    <Level name=\"Year\" column=\"the_year\" type=\"Integer\" internalType=\"int\" uniqueMembers=\"true\"/>\n"
      + "    <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"/>\n"
      + "    <Level name=\"Month\" column=\"month_of_year\" type=\"Numeric\" uniqueMembers=\"false\"/>\n"
      + "   </Hierarchy>\n"
      + "  </Dimension>\n"
      + "  <Dimension name=\"Store\" foreignKeyTable=\"store\" foreignKey=\"store_id\">\n"
      + "    <Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
      + "      <Join leftKey=\"store_country\" rightKey=\"nation_name\">\n"
      + "      <Table name=\"store\"/>\n"
      + "        <InlineTable alias=\"nation\">\n"
      + "          <ColumnDefs>\n"
      + "            <ColumnDef name=\"nation_name\" type=\"String\"/>\n"
      + "            <ColumnDef name=\"nation_shortcode\" type=\"String\"/>\n"
      + "          </ColumnDefs>\n"
      + "          <Rows>\n"
      + "            <Row>\n"
      + "              <Value column=\"nation_name\">USA</Value>\n"
      + "              <Value column=\"nation_shortcode\">US</Value>\n"
      + "            </Row>\n"
      + "            <Row>\n"
      + "              <Value column=\"nation_name\">Mexico</Value>\n"
      + "              <Value column=\"nation_shortcode\">MX</Value>\n"
      + "            </Row>\n"
      + "            <Row>\n"
      + "              <Value column=\"nation_name\">Canada</Value>\n"
      + "              <Value column=\"nation_shortcode\">CA</Value>\n"
      + "            </Row>\n"
      + "          </Rows>\n"
      + "        </InlineTable>\n"
      + "      </Join>\n"
      + "      <Level name=\"Store Country\" table=\"nation\" column=\"nation_name\" nameColumn=\"nation_shortcode\" uniqueMembers=\"true\"/>\n"
      + "      <Level name=\"Store State\" table=\"store\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
      + "      <Level name=\"Store City\" table=\"store\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
      + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\" uniqueMembers=\"true\"/>\n"
      + "    </Hierarchy>\n"
      + "  </Dimension>\n"
      + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
      + "      formatString=\"Standard\" visible=\"false\"/>\n"
      + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
      + "      formatString=\"#,###.00\"/>\n"
      + "</Cube>"
      + "</Schema>" ).verify( expectation );
  }
}
