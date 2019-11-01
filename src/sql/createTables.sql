CREATE TABLE IF NOT EXISTS users(
  id SERIAL PRIMARY KEY,
  username varchar(50) DEFAULT '',
  password TEXT DEFAULT '',
  salt TEXT DEFAULT '',
  account_type VARCHAR(10) DEFAULT '',
  active boolean DEFAULT false
);

INSERT INTO users VALUES(DEFAULT, 'maxwell', 'test', 'test', 'admin');
INSERT INTO users VALUES(DEFAULT, 'brody', 'test', 'test', 'admin');

CREATE TABLE IF NOT EXISTS messages(
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id),
  message TEXT
);
