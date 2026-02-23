<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, java.util.Map" %>
<html>
<head><title>Cautare Student</title></head>
<body>
<h3>Cautare Student</h3>

<form action="./search-student" method="get">
    Cauta dupa nume sau prenume:
    <input type="text" name="query" value="<%= request.getAttribute("query") != null ? request.getAttribute("query") : "" %>"/>
    <button type="submit">Cauta</button>
</form>

<p><a href="./read-student">Inapoi la lista</a></p>

<%
    List<Map<String, Object>> studenti = (List<Map<String, Object>>) request.getAttribute("studenti");
    String query = (String) request.getAttribute("query");
    if (query != null && !query.trim().isEmpty()) {
        if (studenti == null || studenti.isEmpty()) {
%>
<p>Nu a fost gasit niciun student.</p>
<%
        } else {
%>
<table border="1">
    <tr>
        <th>ID</th>
        <th>Nume</th>
        <th>Prenume</th>
        <th>Varsta</th>
    </tr>
    <%
        for (Map<String, Object> s : studenti) {
    %>
    <tr>
        <td><%= s.get("id") %></td>
        <td><%= s.get("nume") %></td>
        <td><%= s.get("prenume") %></td>
        <td><%= s.get("varsta") %></td>
    </tr>
    <%
        }
    %>
</table>
<%
        }
    }
%>
</body>
</html>
