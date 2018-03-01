### Steps to install in Eclipse
1. Navigate to your eclipse workspace folder (eg. `cd ~/eclipse`)
2. `git clone https://github.com/oeoeaio/emma.git`
3. Add a new Java Project in eclipse
4. Use the folder you just cloned into as the project root
5. Select 'Use project folder as root for source and class files'
6. Click 'Next' and make sure it has picked up your `src` folder

### Steps to install the MySQL schema (command line)
1. Install MySQL or MariaDB on your local machine
2. Login using your root credentials: `mysql -u root`
3. Enter your password if required
4. You should be given access to the mysql prompt
5. Create a new database to hold the schema: `CREATE DATABASE enduse;`
6. Note that you can replace `enduse` with whatever name you require
7. Exit from the mysql prompt: `exit;`
8. Load the most recent schema from inside the `schemas` directory of the cloned repository: `mysql -u root -p enduse < [PATH TO YOUR ECLIPSE WORKSPACE]/emma/schemas/EMMA_DB_STRUCTURE_XXXX_XX_XX.sql`
9. Note that you should replace `enduse` with whatever name you used in step 5.

### Installing the schema using a UI
There are plenty of software packages which provide the ability to create databases and load schemas via a user interface. If this sounds preferable, I recommend phpMyAdmin, MySQL Workbench, or HeidiSQL.

### Running the software
You can compile a version of the software yourself from the source code using Eclipse. The only dependency that needs to be installed is the Connector/J library available from the [MySQL website](https://dev.mysql.com/downloads/connector/j/). Alternatively you can just use the pre-packaged `emma.jar` file in the root directory of this repository.
