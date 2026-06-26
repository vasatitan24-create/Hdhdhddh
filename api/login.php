<?php
// api/login.php
require_once '../db.php';
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER["REQUEST_METHOD"] === "POST") {
    // Получаем JSON-данные из тела запроса
    $rawInput = file_get_contents('php://input');
    $data = json_decode($rawInput, true);

    $email = trim($data['email'] ?? '');
    $password = $data['password'] ?? '';

    if (empty($email) || empty($password)) {
        http_response_code(400);
        echo json_encode(['error' => 'Заполните все поля.']);
        exit();
    }

    try {
        $stmt = $pdo->prepare("SELECT * FROM users WHERE email = ?");
        $stmt->execute([$email]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($user && password_verify($password, $user['password'])) {
            // Успешный вход — сохраняем сессию
            $_SESSION['user_id'] = $user['id'];
            $_SESSION['username'] = $user['username'];

            echo json_encode([
                'status' => 'success',
                'username' => $user['username'],
                'role' => $user['role']
            ], JSON_UNESCAPED_UNICODE);
            exit();
        } else {
            http_response_code(401);
            echo json_encode(['error' => 'Неверный email или пароль.']);
            exit();
        }
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Внутренняя ошибка сервера.']);
        exit();
    }
} else {
    http_response_code(405);
    echo json_encode(['error' => 'Метод не поддерживается.']);
    exit();
}
