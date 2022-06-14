/*
* The URL of the W3C Web of Things Thing Description of a lab environment
* Simulated lab URL: "https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab.ttl"
* Real lab URL: "https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab.ttl"
*/
learning_lab_environment("https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab.ttl").
//learning_lab_environment("https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab-real.ttl").
task_requirements([2,3]).

!start.

@start
+!start : learning_lab_environment(Url) & task_requirements([Z1Level, Z2Level])
<-
  .print("Lab environment URL: ", Url);
  .print("I want to achieve Z1Level=", Z1Level, " and Z2Level=",Z2Level);
  makeArtifact("qlearnerArt", "tools.QLearner", [Url], QlearnerArtId);
  focus(QlearnerArtId);
  calculateQ([Z1Level, Z2Level], 3, 0.9, 0.7, 0.1, 100);

  makeArtifact("thingArt", "wot.ThingArtifact", [Url], ThingArtId);
  focus(ThingArtId);

  !action.

+!action: task_requirements([Z1Level, Z2Level])
<-
  .print("Getting current state");
  getCurrentState(State);
  .print("Goal state ", [Z1Level, Z2Level], " Current state ", State);
  getActionFromState([Z1Level, Z2Level], State, ActionTag, PayloadTags, Payload);
  invokeAction(ActionTag, PayloadTags, Payload);
  .wait(60000);
  !action.