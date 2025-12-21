# Introduction
Read the files `README.md` and `.codex/AGENTS.md` to get context for this project.
Also read the files in thoe `docs` folder.
When implementing the following tasks I want you to only do one task at a time
and when done prompt me what to do next.
A task is done when the created code is properly covered by one or more tests
and all tests of the project run successfully.

# Tasks
## Task 1
The entities (e.g. the Voucher) currently accepts a `discountJson` property as an arbitrary map 
in a jsonb column.
I want you to create a folder
`api/dto` that contains DataTransferObject that will be passed over the API and then stored
in the jsonb. Please take a look at the markdown files in the docs folder on how these
data classes shall look like. Make sure that the classes can be deserialized and put into
the database column. Also create the needed repositories for the entities.
Create tests that make sure that entities can be read, created, updated, deleted.

## Task 2
Add Spring controllers for the endpoint outlined in the markdown files in the `docs` folder.
Also add documentation annotations using Spring Doc. It is probably necessary to extend
the build.gradle.kts. Make sure that a recent version of Spring Doc is used.
Do not wire the controllers to services, yet.
Create tests for the endpoints that are exposed by the controllers

## Task 3
Create the services layer.
The services shall implement the business logic as available with voucherify.
Focus on the business logic needed to implement the use cases outlined in the `docs` folder.
Write tests for the services themselves.
When done also create end-to-end tests using the H2 database.
