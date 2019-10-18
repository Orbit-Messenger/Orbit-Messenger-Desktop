CREATE TABLE IF NOT EXISTS users(
  id SERIAL PRIMARY KEY,
  username varchar(50) NOT NULL,
  password varchar(50) NOT NULL
);

INSERT INTO users VALUES(DEFAULT, 'maxwell', 'test');
INSERT INTO users VALUES(DEFAULT, 'brody', 'test');

CREATE TABLE IF NOT EXISTS messages(
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id),
  message TEXT
);
