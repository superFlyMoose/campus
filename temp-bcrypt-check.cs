using System; using BCrypt.Net; class T { static void Main() { Console.Write(BCrypt.Net.BCrypt.Verify("123456", "$10$e0NRP4sQx0mVxS1l3m5k4.e7xR4Q4mG0k2QWJkK6K0N8Vx1Y3qN5O")); } }
