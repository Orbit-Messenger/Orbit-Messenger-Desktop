CREATE TABLE IF NOT EXISTS avatars(
  id SERIAL PRIMARY KEY,
  version INT NOT NULL DEFAULT floor(random() * 100 +1),
  location VARCHAR(100) DEFAULT './src/res/images/default.jpg'
);

INSERT INTO avatars VALUES(DEFAULT, DEFAULT, DEFAULT);

CREATE TABLE IF NOT EXISTS users(
  id SERIAL PRIMARY KEY,
  username varchar(50) DEFAULT '',
  password TEXT DEFAULT '',
  salt TEXT DEFAULT '',
  status boolean DEFAULT false,
  room VARCHAR(50) DEFAULT 'general',
  avatar_id INT REFERENCES avatars(id) DEFAULT 1
  );

INSERT INTO users VALUES(DEFAULT, 'maxwell', 'test', 'test');
INSERT INTO users VALUES(DEFAULT, 'brody', 'test', 'test');
INSERT INTO users VALUES(DEFAULT, 'admin', 'test', 'test');
INSERT INTO users VALUES(DEFAULT, 'test', 'test', 'test');

CREATE TABLE IF NOT EXISTS chatrooms(
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL
);

INSERT INTO chatrooms VALUES(DEFAULT, 'general');
INSERT INTO chatrooms VALUES(DEFAULT, 'programming');
INSERT INTO chatrooms VALUES(DEFAULT, 'stuff');
INSERT INTO chatrooms VALUES(DEFAULT, 'direct_messages');

CREATE TABLE IF NOT EXISTS messages(
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id) NOT NULL,
  received_user_id INT REFERENCES users(id) DEFAULT null,
  chatroom_id INT REFERENCES chatrooms(id) NOT NULL,
  message TEXT NOT NULL,
  time_stamp timestamp DEFAULT NOW() NOT NULL
);

INSERT INTO messages VALUES(DEFAULT, 1, null, 1, 'general 1');
INSERT INTO messages VALUES(DEFAULT, 2, null, 1, 'general 2');
INSERT INTO messages VALUES(DEFAULT, 3, null, 1, 'general 3');
INSERT INTO messages VALUES(DEFAULT, 4, null, 1, 'general 4');

INSERT INTO messages VALUES(DEFAULT, 1, null, 2, 'programming 1');
INSERT INTO messages VALUES(DEFAULT, 2, null, 2, 'programming 2');
INSERT INTO messages VALUES(DEFAULT, 3, null, 2, 'programming 3');
INSERT INTO messages VALUES(DEFAULT, 4, null, 2, 'programming 4');

INSERT INTO messages VALUES(DEFAULT, 1, null, 3, 'stuff 1');
INSERT INTO messages VALUES(DEFAULT, 2, null, 3, 'stuff 2');
INSERT INTO messages VALUES(DEFAULT, 3, null, 3, 'stuff 3');
INSERT INTO messages VALUES(DEFAULT, 4, null, 3, 'stuff 4');

INSERT INTO messages VALUES(DEFAULT, 1, 2, 4, 'direct message 1');
INSERT INTO messages VALUES(DEFAULT, 2, 1, 4, 'direct message 2');
INSERT INTO messages VALUES(DEFAULT, 3, 4, 4, 'direct message 3');
INSERT INTO messages VALUES(DEFAULT, 4, 3, 4, 'direct message 4');

CREATE VIEW full_avatar_info AS
SELECT users.username, avatars.version, avatars.location
FROM avatars
INNER JOIN users ON users.avatar_id = avatars.id;

CREATE VIEW full_messages AS 
SELECT messages.id, users.username, chatrooms.name, messages.message, messages.time_stamp 
FROM messages 
INNER JOIN users ON users.id = messages.user_id 
INNER JOIN chatrooms ON chatrooms.id = messages.chatroom_id ORDER BY messages.id ASC;


CREATE VIEW full_direct_messages AS 
SELECT messages.id, u1.username AS sender, u2.username AS receiver, chatrooms.name, messages.message, messages.time_stamp 
FROM messages 
INNER JOIN users u1 ON u1.id = messages.user_id 
LEFT JOIN users u2 ON u2.id = messages.received_user_id
INNER JOIN chatrooms ON chatrooms.id = messages.chatroom_id 
WHERE chatrooms.name ='direct_messages'
ORDER BY messages.id ASC;
