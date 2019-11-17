CREATE TABLE IF NOT EXISTS users(
  id SERIAL PRIMARY KEY,
  username varchar(50) DEFAULT '',
  password TEXT DEFAULT '',
  salt TEXT DEFAULT '',
  account_type VARCHAR(10) DEFAULT '',
  status boolean DEFAULT false
);

INSERT INTO users VALUES(DEFAULT, 'maxwell', 'test', 'test', 'admin');
INSERT INTO users VALUES(DEFAULT, 'brody', 'test', 'test', 'admin');
INSERT INTO users VALUES(DEFAULT, 'admin', 'test', 'test', 'admin');
INSERT INTO users VALUES(DEFAULT, 'test', 'test', 'test', 'user');

CREATE TABLE IF NOT EXISTS messages(
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id),
  message TEXT,
  time_stamp timestamp
);

INSERT INTO messages VALUES(DEFAULT, 1, 'testing 1', 'now');
INSERT INTO messages VALUES(DEFAULT, 2, 'testing 2', 'now');
INSERT INTO messages VALUES(DEFAULT, 3, 'testing 3', 'now');
INSERT INTO messages VALUES(DEFAULT, 4, 'testing 4', 'now');
