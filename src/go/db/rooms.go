package db

import "context"

func (dbConn DatabaseConnection) GetAllRooms() ([]string, error) {
	var rooms []string
	rows, err := dbConn.conn.Query(context.Background(),
		"SELECT name FROM chatrooms")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	for rows.Next() {
		var room string
		err = rows.Scan(&room)
		if err != nil {
			return nil, err
		}
		rooms = append(rooms, room)
	}
	return rooms, nil
}
