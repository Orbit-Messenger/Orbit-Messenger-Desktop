CREATE TABLE IF NOT EXISTS users(
  id SERIAL PRIMARY KEY,
  username varchar(50) DEFAULT '',
  password TEXT DEFAULT '',
  salt TEXT DEFAULT '',
  status boolean DEFAULT false,
  room VARCHAR(50) DEFAULT 'general'
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

CREATE TABLE IF NOT EXISTS messages(
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id) NOT NULL,
  received_user_id INT REFERENCES users(id),
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

CREATE VIEW full_messages AS SELECT messages.id, users.username, chatrooms.name, messages.message, messages.time_stamp FROM messages INNER JOIN users ON users.id = messages.user_id INNER JOIN chatrooms ON chatrooms.id = messages.chatroom_id ORDER BY messages.id ASC;


