<?php
// api/get_chats.php
require_once '../db.php';
header('Content-Type: application/json; charset=utf-8');

// Проверяем авторизацию пользователя в PHP-сессии
if (!isset($_SESSION['username'])) {
    http_response_code(401);
    echo json_encode(['error' => 'Пользователь не авторизован']);
    exit();
}

$currentUser = $_SESSION['username'];

try {
    // Получаем последние сообщения из каждого активного диалога
    $query = "
    SELECT 
        pm.id, pm.sender, pm.receiver, pm.message, pm.created_at, pm.is_read,
        u.avatar_type AS contact_avatar_type,
        CASE WHEN pm.sender = :u THEN pm.receiver ELSE pm.sender END AS contact_name
    FROM private_messages pm
    INNER JOIN (
        SELECT 
            CASE WHEN sender = :u THEN receiver ELSE sender END AS contact_name,
            MAX(id) AS max_id
        FROM private_messages
        WHERE (receiver = :u AND hidden_by_receiver = 0)
           OR (sender = :u AND hidden_by_sender = 0)
        GROUP BY 1
    ) t ON pm.id = t.max_id
    LEFT JOIN users u ON u.username = (CASE WHEN pm.sender = :u THEN pm.receiver ELSE pm.sender END)
    ORDER BY pm.created_at DESC
    ";

    $stmt = $pdo->prepare($query);
    $stmt->execute(['u' => $currentUser]);
    $dialogs = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Возвращаем приложению чистый JSON
    echo json_encode($dialogs, JSON_UNESCAPED_UNICODE);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Ошибка базы данных: ' . $e->getMessage()]);
}
