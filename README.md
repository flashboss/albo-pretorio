A set of utilities to use an albo pretorio online

Albo imported through the command:

./export.sh -user admin -pwd admin -s workspace://SpacesStore -p /app:company_home/cm:Albo -verbose -root -zip Albo

need to set the ALF_HOME with the path of the installed alfresco 5.1.g

to start the demo:

./run.sh

then, open the browser and connect to http://localhost:8080/alfresco/

to test or install the demo:

mvn clean install -Ddependency.surf.version=6.3

The publishing console is under: http://127.0.0.1:8080/share/page/site/albopretorio/documentlibrary
You can access with the user: mjackson/mjackson

The guest web site is under: http://127.0.0.1:8080/share/page/dp/ws/simple-page
