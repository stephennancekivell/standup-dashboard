Standup Dashboard
=================

* list team members in order of their turn to run the standup meeting.

* list team
* mark done, send from top of queue to bottom.
* add person
* skip person, send from middle of the queue to the bottom.
* skip, send to bottom of queue.


GET /api/members
["Bill","Bob","Bridget"]

POST /api/members
"Beth"
delete /api/members
"Beth"

Post /api/standup?done-by=Bill
Post /api/standup?send-to-back=Bill