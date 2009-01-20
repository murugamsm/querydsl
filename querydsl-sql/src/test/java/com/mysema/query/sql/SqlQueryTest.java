/*
 * Copyright (c) 2008 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.query.sql;

import static com.mysema.query.grammar.Grammar.avg;
import static com.mysema.query.grammar.Grammar.count;
import static com.mysema.query.grammar.QMath.add;
import static com.mysema.query.grammar.QMath.max;
import static com.mysema.query.grammar.QMath.min;
import static com.mysema.query.grammar.SqlGrammar.select;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.mysema.query.grammar.Dialect;
import com.mysema.query.grammar.QMath;
import com.mysema.query.grammar.SqlJoinMeta;
import com.mysema.query.grammar.SqlOps;
import com.mysema.query.grammar.types.Expr;
import com.mysema.query.grammar.types.SubQuery;
import com.mysema.query.sql.domain.QEMPLOYEE;
import com.mysema.query.sql.domain.QSURVEY;
import com.mysema.query.sql.domain.QTEST;
import com.mysema.query.sql.dto.IdName;
import com.mysema.query.sql.dto.QIdName;


/**
 * SqlQueryTest provides
 *
 * @author tiwe
 * @version $Id$
 */
public class SqlQueryTest {
    
    private static final SqlOps dialect = Dialect.forHqlsdb().newLineToSingleSpace();
    
    private QEMPLOYEE employee = new QEMPLOYEE("employee");
    private QSURVEY survey = new QSURVEY("survey");    
    private QSURVEY survey2 = new QSURVEY("survey2");    
    private QTEST test = new QTEST("test");
    
    private static Connection c;
    
    private static Statement stmt;
    
    private String expectedQuery;
    
    @BeforeClass
    public static void setUp() throws Exception{
        String sql;
        c = getHSQLConnection();
        stmt = c.createStatement();
        
        // survey
        stmt.execute("drop table survey if exists");
        stmt.execute("create table survey (id int,name varchar(30));");
        stmt.execute("insert into survey values (1, 'Hello World');");
        
        // test
        stmt.execute("drop table test if exists");
        stmt.execute("create table test(name varchar(255))");
        sql   = "insert into test values(?)";
        PreparedStatement pstmt = c.prepareStatement(sql);
        for (int i = 0; i < 10000; i++) {
            pstmt.setString(1, "name" + i);
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        
        // employee
        stmt.execute("drop table employee if exists");
        stmt.execute("create table employee(id int, "
                + "firstname VARCHAR(50), " + "lastname VARCHAR(50), "
                + "salary decimal(10, 2), " + "superior_id int, "
                + "CONSTRAINT PK_employee PRIMARY KEY (id), "
                + "CONSTRAINT FK_superior FOREIGN KEY (superior_id) "
                + "REFERENCES employee(ID))");
        addEmployee(1, "Mike", "Smith", 160000, -1);
        addEmployee(2, "Mary", "Smith", 140000, -1);

        // Employee under Mike
        addEmployee(10, "Joe", "Divis", 50000, 1);
        addEmployee(11, "Peter", "Mason", 45000, 1);
        addEmployee(12, "Steve", "Johnson", 40000, 1);
        addEmployee(13, "Jim", "Hood", 35000, 1);

        // Employee under Mike
        addEmployee(20, "Jennifer", "Divis", 60000, 2);
        addEmployee(21, "Helen", "Mason", 50000, 2);
        addEmployee(22, "Daisy", "Johnson", 40000, 2);
        addEmployee(23, "Barbara", "Hood", 30000, 2);
        
        // date_test and time_test
        stmt.execute("drop table time_test if exists");
        stmt.execute("drop table date_test if exists");
        stmt.execute("create table time_test(time_test time)");
        stmt.execute("create table date_test(date_test date)");

    }
    
    private static void addEmployee(int id, String firstName, String lastName,
            double salary, int superiorId) throws Exception {
        stmt.execute("insert into employee values(" + id + ", '" + firstName
                + "', '" + lastName + "', " + salary + ", "
                + (superiorId <= 0 ? "null" : ("" + superiorId)) + ")");
    }

    
    @AfterClass
    public static void tearDown() throws Exception{
        if (stmt != null) stmt.close();
        if (c != null) c.close();
    }
    
    @Test
    public void testQuery1() throws Exception{                
        for (String s : q().from(survey).list(survey.name)){
            System.out.println(s);
        }        
    }
    
    @Test
    public void testQuery2() throws Exception{              
        for (Object[] row : q().from(survey).list(survey.id, survey.name)){
            System.out.println(row[0]+", " + row[1]);
        }        
    }
    
    @Test
    public void testQueryWithConstant() throws Exception{
        for (Object[] row : q().from(survey)
                           .where(survey.id.eq(1))
                           .list(survey.id, survey.name)){
            System.out.println(row[0]+", " + row[1]);
        }
    }
    
    @Test
    public void testJoin() throws Exception{
        for (String name : q().from(survey, survey2)
                          .where(survey.id.eq(survey2.id))
                          .list(survey.name)){
            System.out.println(name);
        }
    }
    
    @Test
    public void testConstructor() throws Exception{
        for (IdName idName : q().from(survey)
                            .list(new QIdName(survey.id, survey.name))){
            System.out.println("id and name : " + idName.getId()+ ","+idName.getName());
        }
    }
    
    @Test
    public void testVarious() throws SQLException{
        System.out.println(q().from(survey).list(survey.name.lower()));
        System.out.println(q().from(survey).list(survey.name.add("abc")));
        System.out.println(q().from(survey).list(survey.id.eq(0)));        
        System.out.println(q().from(survey).list(QMath.sqrt(survey.id)));
        
    }
    
    @Test
    public void testSyntaxForTest() throws SQLException{        
        // TEST        
        // select count(*) from test where name = null
        expectedQuery = "select count(*) from test test where test.name is null";
        q().from(test).where(test.name.isnull()).count();        
        // select count(*) from test where name like null
//        q().from(test).where(test.name.like(null)).count();
        // select count(*) from test where name = ''
        q().from(test).where(test.name.like("")).count();        
        // select count(*) from test where name is not null
        q().from(test).where(test.name.isnotnull()).count();
        // select count(*) from test where name like '%'
        q().from(test).where(test.name.like("%")).count();
        // select count(*) from test where left(name, 6) = 'name44'
        q().from(test).where(test.name.substring(0,6).like("name44%")).count();
        // select count(*) from test where name like 'name44%'
        q().from(test).where(test.name.like("name44%")).count();
        // select count(*) from test where left(name,5) = 'name4' and right(name,1) = 5
        // TODO
        // select count(*) from test where name like 'name4%5'
        q().from(test).where(test.name.like("name4%5")).count();
        // select count(*) from test where left(name,5) = 'name4' and right(name,1) = 5
        // TODO
        // select count(*) from test where name like 'name4%5'
        q().from(test).where(test.name.like("name4%5")).count();
    }
    
    @Test
    public void testSyntaxForEmployee() throws SQLException{
        // EMPLOYEE
//        "select avg(salary), max(id) from employee "
//        + "group by superior_id " + "order by superior_id " + "";
        q().from(employee)
           .groupBy(employee.superiorId)
           .orderBy(employee.superiorId.asc())
           .list(avg(employee.salary), max(employee.id));
        
//        "select avg(salary), max(id) from employee "
//        + "group by superior_id " + "having max(id) > 5 "
//        + "order by superior_id " + "";
        q().from(employee)
           .groupBy(employee.superiorId).having(max(employee.id).gt(5))
           .orderBy(employee.superiorId.asc())
           .list(avg(employee.salary), max(employee.id));
        
//        "select avg(salary), max(id) from employee "
//        + "group by superior_id "
//        + "having superior_id is not null "
//        + "order by superior_id " + "";
        q().from(employee)
           .groupBy(employee.superiorId).having(employee.superiorId.isnotnull())
           .orderBy(employee.superiorId.asc())
           .list(avg(employee.salary),max(employee.id));        
    }
    
    @Test
    public void testIllegal() throws SQLException{
//        q().from(employee).list(employee);
    }
    
    @Test
    public void testLimitAndOffset() throws SQLException{
        // limit offset
        expectedQuery = "select employee.id from employee employee limit 4 offset 3";
        q().from(employee).limit(4).offset(3).list(employee.id);        
    }
    
    @Test
    public void testSubQueries() throws SQLException{
     // subquery in where block
        expectedQuery = "select employee.id from employee employee " +
            "where employee.id = (select max(employee.id) " + 
            "from employee employee)";
        List<Integer> list = q().from(employee).where(employee.id.eq(
                select(max(employee.id)).from(employee)))
            .list(employee.id);
        assertFalse(list.isEmpty());
    }
    
    @Test
    public void testIllegalUnion() throws SQLException{
        SubQuery<SqlJoinMeta,Integer> sq1 = select(max(employee.id)).from(employee);
        SubQuery<SqlJoinMeta,Integer> sq2 = select(min(employee.id)).from(employee);
        try{
            q().from(employee).union(sq1, sq2).list();
            fail();
        }catch(IllegalArgumentException e){
            // expected
        }
        
    }
    
    @Test
    public void testUnion() throws SQLException{
        // union
        SubQuery<SqlJoinMeta,Integer> sq1 = select(max(employee.id)).from(employee);
        SubQuery<SqlJoinMeta,Integer> sq2 = select(min(employee.id)).from(employee);
        List<Integer> list = q().union(sq1, sq2).list(); 
        assertFalse(list.isEmpty());
        
        // variation 1
        list = q().union(
            select(max(employee.id)).from(employee), 
            select(min(employee.id)).from(employee)).list();
        assertFalse(list.isEmpty());
    
        // union #2
        SubQuery<SqlJoinMeta,Object[]> sq3 = select(count(), max(employee.id)).from(employee);
        SubQuery<SqlJoinMeta,Object[]> sq4 = select(count(), min(employee.id)).from(employee);
        List<Object[]> list2 = q().union(sq3, sq4).list();
        assertFalse(list2.isEmpty());
    }
    
    @Test
    @Ignore
    public void testQueryWithoutFrom() throws SQLException{
        // NOTE : doesn't work in HSQLDB
        q().list(add(new Expr.EConstant<Integer>(1),1));
    }
    
    @Test
    public void testWhereExists(){
//        q().from(employee).where(exists()
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testFunctions() throws SQLException{
        Expr<Integer> i = new Expr.EConstant<Integer>(1);
        Expr<Double> d = new Expr.EConstant<Double>(1.0);
        for (Expr<?> e : Arrays.<Expr<?>>asList(
                QMath.abs(i),
                QMath.acos(d),
                QMath.asin(d),
                QMath.atan(d),
                QMath.ceil(d),
                QMath.cos(d),
                QMath.tan(d),
                QMath.sqrt(i),
                QMath.sin(d),
                QMath.round(d),
                QMath.random(),
                QMath.pow(d,d),
//                QMath.min(i,i),
//                QMath.max(i,i),
                QMath.mod(i,i),
                QMath.log10(d),
                QMath.log(d),
                QMath.floor(d),
                QMath.exp(d))){
            q().from(employee).list((Expr<? extends Comparable>)e);
        }
    }
    
    private SqlQuery q(){
        return new SqlQuery(c, dialect){
            @Override
            protected String buildQueryString() {
                String rv = super.buildQueryString();
                if (expectedQuery != null){
                   assertEquals(expectedQuery, rv);
                   expectedQuery = null;
                }
                System.out.println(rv);
                return rv;
            }
        };
    }
    
    private static Connection getHSQLConnection() throws Exception{
        Class.forName("org.hsqldb.jdbcDriver");
        String url = "jdbc:hsqldb:data/tutorial";
        return DriverManager.getConnection(url, "sa", "");
    }

}
