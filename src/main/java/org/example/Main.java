package org.example;

import java.sql.*;
import java.time.LocalDateTime;

public class Main {
    public static void main(String args[]){
        //Class.forName() 생략 가능
        String url = "jdbc:mysql://localhost:3307/crowdfunding";;
        String query = "SELECT * FROM project";
        String preQuery1 = "UPDATE BOARD SET contents =? WHERE id = ?"; // 동적 쿼리
        String preQuery2 = "INSERT INTO board(title,writer,contents,regdate,hit) VALUES (?,?,?,?,?)";


        try (
                Connection conn = DriverManager.getConnection(url, "hyjun", "0636");
                //Statement stmt = conn.createStatement();
                //ResultSet rs = stmt.executeQuery(query)
                PreparedStatement preparedStatement = conn.prepareStatement(query);
                //ResultSet rs = preparedStatement.executeQuery();
        ) {
            boolean result = preparedStatement.execute();

            if (result) {
                try (ResultSet rs = preparedStatement.getResultSet()) {

                    while (rs.next()) {
                        int projectId = rs.getInt("project_id");
                        int userId = rs.getInt("user_id");

                        String title = rs.getString("title");
                        String subtitle = rs.getString("subtitle");
                        String introduction = rs.getString("introduction");
                        String itemInfo = rs.getString("item_info");

                        int goalAmount = rs.getInt("goal_amount");
                        String category = rs.getString("category");
                        String projectStatus = rs.getString("project_status");

                        LocalDateTime fundingStartAt =
                                rs.getTimestamp("funding_start_at").toLocalDateTime();

                        LocalDateTime fundingEndAt =
                                rs.getTimestamp("funding_end_at").toLocalDateTime();

                        LocalDateTime paymentDueAt =
                                rs.getTimestamp("payment_due_at").toLocalDateTime();

                        LocalDateTime deliveryStartAt =
                                rs.getTimestamp("delivery_start_at").toLocalDateTime();

                        System.out.printf(
                                "프로젝트 ID: %d%n" +
                                        "사용자 ID: %d%n" +
                                        "제목: %s%n" +
                                        "부제목: %s%n" +
                                        "소개: %s%n" +
                                        "상품 정보: %s%n" +
                                        "목표 금액: %d%n" +
                                        "카테고리: %s%n" +
                                        "상태: %s%n" +
                                        "펀딩 시작: %s%n" +
                                        "펀딩 종료: %s%n" +
                                        "결제 예정: %s%n" +
                                        "배송 시작: %s%n",
                                projectId, userId, title, subtitle,
                                introduction, itemInfo, goalAmount,
                                category, projectStatus,
                                fundingStartAt, fundingEndAt,
                                paymentDueAt, deliveryStartAt
                        );

                        System.out.println("-------------------------------------");
                    }
                }
            }

            /*
            //pstmt select
            boolean result = preparedStatement.execute();

            if (result) {
                try (ResultSet rs = preparedStatement.getResultSet()) {

                    while (rs.next()) {
                        int projectId = rs.getInt("project_id");
                        int userId = rs.getInt("user_id");

                        String title = rs.getString("title");
                        String subtitle = rs.getString("subtitle");
                        String introduction = rs.getString("introduction");
                        String itemInfo = rs.getString("item_info");

                        int goalAmount = rs.getInt("goal_amount");
                        String category = rs.getString("category");
                        String projectStatus = rs.getString("project_status");

                        Timestamp fundingStartTimestamp =
                                rs.getTimestamp("funding_start_at");
                        LocalDateTime fundingStartAt =
                                fundingStartTimestamp != null
                                        ? fundingStartTimestamp.toLocalDateTime() : null;

                        Timestamp fundingEndTimestamp =
                                rs.getTimestamp("funding_end_at");
                        LocalDateTime fundingEndAt =
                                fundingEndTimestamp != null
                                        ? fundingEndTimestamp.toLocalDateTime() : null;

                        Timestamp paymentDueTimestamp =
                                rs.getTimestamp("payment_due_at");
                        LocalDateTime paymentDueAt =
                                paymentDueTimestamp != null
                                        ? paymentDueTimestamp.toLocalDateTime() : null;

                        Timestamp deliveryStartTimestamp =
                                rs.getTimestamp("delivery_start_at");
                        LocalDateTime deliveryStartAt =
                                deliveryStartTimestamp != null
                                        ? deliveryStartTimestamp.toLocalDateTime() : null;

                        System.out.printf(
                                "프로젝트 ID: %d%n" +
                                        "사용자 ID: %d%n" +
                                        "제목: %s%n" +
                                        "부제목: %s%n" +
                                        "소개: %s%n" +
                                        "상품 정보: %s%n" +
                                        "목표 금액: %d%n" +
                                        "카테고리: %s%n" +
                                        "상태: %s%n" +
                                        "펀딩 시작: %s%n" +
                                        "펀딩 종료: %s%n" +
                                        "결제 예정: %s%n" +
                                        "배송 시작: %s%n",
                                projectId, userId, title, subtitle,
                                introduction, itemInfo, goalAmount,
                                category, projectStatus,
                                fundingStartAt, fundingEndAt,
                                paymentDueAt, deliveryStartAt
                        );

                        System.out.println("-------------------------------------");
                    }
                }
            }
            */



        } catch(SQLException e){
            System.out.println("error : " + e);
        }
    }
}