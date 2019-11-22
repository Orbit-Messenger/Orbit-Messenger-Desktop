package routes

import (
	"fmt"
	//"log"
)

type Action struct {
	Action    string
	MessageId int64
}

type ServerActionsController struct {
	Actions     []Action
	ActionCount int64
}

func CreateServerActionsController() *ServerActionsController {
	return &ServerActionsController{
		*new([]Action),
		0,
	}
}

func (sac *ServerActionsController) addAction(action Action) {
	sac.Actions = append(sac.Actions, action)
	sac.ActionCount++
}

func (sac *ServerActionsController) AddDeleteAction(messageId int64) {
	sac.addAction(Action{
		"delete",
		messageId,
	})
}

func (sac ServerActionsController) GetNewestAction() (Action, error) {
	if sac.ActionCount < 1 {
		return *new(Action), fmt.Errorf("no actions")
	}
	return sac.Actions[len(sac.Actions)-1], nil
}
