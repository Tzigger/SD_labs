<html xmlns:jsp="http://java.sun.com/JSP/Page">
	<head>
		<title>Adauga student</title>
		<meta charset="UTF-8" />
	</head>
	<body>
		<h3>Adauga student nou</h3>
		<form action="./process-student" method="post">
			Nume: <input type="text" name="nume" />
			<br />
			Prenume: <input type="text" name="prenume" />
			<br />
			Varsta: <input type="number" name="varsta" />
			<br />
			<br />
			<button type="submit">Adauga</button>
		</form>
		<p><a href="./read-student">Inapoi la lista</a></p>
	</body>
</html>