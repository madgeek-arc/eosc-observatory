## [4.1.0](https://github.com/madgeek-arc/eosc-observatory/compare/4.0.0...4.1.0) (2026-02-19)

### Features

* Adds creation date in thread and returns threads ordered on get methods ([e53902e](https://github.com/madgeek-arc/eosc-observatory/commit/e53902e966a9af4428273f4655de31afb6631380))
* Adds model name in metadata and functionality to regenerate document if different model is set ([e83ab4d](https://github.com/madgeek-arc/eosc-observatory/commit/e83ab4d336db55cdda54d394beaeb857b96a951b))
* Adds submission start/close dates to SurveyAnswerInfo. ([efe07fe](https://github.com/madgeek-arc/eosc-observatory/commit/efe07fea1a53efb53be03a9a39257b6bc68c7336))
* Adds target to comments and mentions to messages ([6772453](https://github.com/madgeek-arc/eosc-observatory/commit/67724535ff9bdab488308c4b80134eb8c8ea2c54))
* Creates domain classes and persistence for commenting ([36570d1](https://github.com/madgeek-arc/eosc-observatory/commit/36570d14d7db122fbd2e0dee25163d69034d920f))
* Enables authorization when retrieving documents from resources-registry ([7fc4b8a](https://github.com/madgeek-arc/eosc-observatory/commit/7fc4b8a72f3a2331975d0421b7d02ca0b958d94a))
* Introduces csrf protection ([37124a4](https://github.com/madgeek-arc/eosc-observatory/commit/37124a418e692ea942a52d442e9a6106bf3f9b6f))
* Members of a group are sorted ([c35323d](https://github.com/madgeek-arc/eosc-observatory/commit/c35323d0c81a0b9eca5fbe976a88a1a671c22064))
* Public method returning stakeholder managers' basic information ([3bfeb82](https://github.com/madgeek-arc/eosc-observatory/commit/3bfeb829cc256995c3283d48bda5905adc3cd6ef))
* Resources registry document generation for surveyId ([3511e10](https://github.com/madgeek-arc/eosc-observatory/commit/3511e100d96bb0dbc3529af7df269137af3a3d15))
* **resources-registry,recommendations:** Implements recommendation mechanism for resources-registry ([7a89c50](https://github.com/madgeek-arc/eosc-observatory/commit/7a89c507504f8d6ce174d135f07e70dd2d1d842d))
* Updates catalogue dependency ([17b783b](https://github.com/madgeek-arc/eosc-observatory/commit/17b783b09757b601d4dab72d6484f4710ce92daa))

### Bug Fixes

* Adds authorization to non-approved document recommendations ([59b74eb](https://github.com/madgeek-arc/eosc-observatory/commit/59b74eb801d8b799b41d0ffe33705b3a00984826))
* Adds exception handler for failures ([52925ba](https://github.com/madgeek-arc/eosc-observatory/commit/52925ba7145070dc5690427cea97903bf926cfa2))
* adds json annotations to fix comment creation ([2a9b5fc](https://github.com/madgeek-arc/eosc-observatory/commit/2a9b5fc57102de593f7f6116072e4b6d7a7361c7))
* Adds property fixing registry dump ([1fd0329](https://github.com/madgeek-arc/eosc-observatory/commit/1fd0329042fb468865f9739d2ddbddcf62872bda))
* Administrators can retrieve survey answers info ([28c8ae0](https://github.com/madgeek-arc/eosc-observatory/commit/28c8ae06ce253eb6eb744d08a59ae2d99bc7d7fe))
* Assigns permission to admins to generate answers ([43fba43](https://github.com/madgeek-arc/eosc-observatory/commit/43fba437ef33e01b8d01c92f824c02905ea40eb8))
* Bumps registry dependency version ([5f9c9b9](https://github.com/madgeek-arc/eosc-observatory/commit/5f9c9b945436d8e4faebcf6e8e4b59d0f0ac34e2))
* Calls save() method when editing a comment ([d6f84c0](https://github.com/madgeek-arc/eosc-observatory/commit/d6f84c0410148bb0744a4cfd2e93c4fc6b995496))
* Changes user group permissions ([c5fd08a](https://github.com/madgeek-arc/eosc-observatory/commit/c5fd08a01084062067d1efab186a1e208daabc70))
* Checks for null mentions ([e3cba66](https://github.com/madgeek-arc/eosc-observatory/commit/e3cba6607c575ba3cafbd914bf70b2572d07c55b))
* Correction in security expression ([cae1ccc](https://github.com/madgeek-arc/eosc-observatory/commit/cae1ccc3bdd3f44af6592e8b854208fed9e995a4))
* corrects targetId type from UUID to String ([be678c6](https://github.com/madgeek-arc/eosc-observatory/commit/be678c65f81f7706f4e63afcff4c10ff1dc76df8))
* Corrects topic sending deleted comment threads ([1835c51](https://github.com/madgeek-arc/eosc-observatory/commit/1835c519a5eebfe49adc81ba1cd79cfc39327fa7))
* Creates cookie with csrf token and enables csrf in swagger-ui ([d8ce636](https://github.com/madgeek-arc/eosc-observatory/commit/d8ce636802070627a701fd2a99ddd303f6a85940))
* Creates metadata class specifically for the resources registry ([e440329](https://github.com/madgeek-arc/eosc-observatory/commit/e44032966219ba059ac012dd97a57b758abe4a2b))
* Deleting comment thread sends to separate topic ([197515b](https://github.com/madgeek-arc/eosc-observatory/commit/197515b7fb273cc04b2a1a7e28ea8ef8aadd2259))
* Fixes case where infinite loop might occur ([ffb5666](https://github.com/madgeek-arc/eosc-observatory/commit/ffb5666c68ebced6ad63006f9b60411f1bd992d8))
* Public method returning stakeholder managers ignores users with missing name and surname ([76e6dcf](https://github.com/madgeek-arc/eosc-observatory/commit/76e6dcf382c9f64fdebd3cd177e4bd40d6f5c9fc))
* Secures generic controller ([a3884a9](https://github.com/madgeek-arc/eosc-observatory/commit/a3884a9c10660a0bc5428bd767d773f0394910d8))
* Secures methods retrieving documents by id and migrating to another resource type ([356ff88](https://github.com/madgeek-arc/eosc-observatory/commit/356ff88ef389212bbd4e437c37bfee98e2a5a282))
* Secures update/delete actions on comments ([4aa5949](https://github.com/madgeek-arc/eosc-observatory/commit/4aa5949020315bb686bbb900fb60e9c922bf7999))
* **security:** Removes access token from cookie ([0feccf9](https://github.com/madgeek-arc/eosc-observatory/commit/0feccf90eac7fb9f27cd5c953439816d4ea15be5))
* Updates registry dependency fixing issue of lazy initialization of resource type aliases - removed workaround properties ([3da55db](https://github.com/madgeek-arc/eosc-observatory/commit/3da55dbbe302cee838d67cfebcea1d9c5db4c8cf))
* Updates resources-registry document generation template ([0dd3149](https://github.com/madgeek-arc/eosc-observatory/commit/0dd3149e448f2d1239754904eee2e6e29b1e3137))
* Updating a document sets 'curated' flag to true ([7059b69](https://github.com/madgeek-arc/eosc-observatory/commit/7059b69426a7c1fb7097a4d94ea8a26b2a942bc4))
* Updating a message method uses findWithCommentById() method which initializes the comment thread - fixes lazy initialization outside of session when mapping to dto ([ba39258](https://github.com/madgeek-arc/eosc-observatory/commit/ba39258da2ccaceb1096d6d629d364aff20ad48f))

## [4.3.0](https://github.com/madgeek-arc/eosc-observatory/compare/4.4.2...4.3.0) (2026-09-23)


### ⚠ BREAKING CHANGES

* Change API responses to json
* change group id and package names

### Features

* add method returning public news of a stakeholder ([dd6e599](https://github.com/madgeek-arc/eosc-observatory/commit/dd6e59945d28ac5aa923a54baaf9f02324f2ea01))
* add method returning views for the provided path ([d7add13](https://github.com/madgeek-arc/eosc-observatory/commit/d7add13aa050f747d0d782b9dd02272d8089f818))
* add most informative paragraphs to Document ([cc01010](https://github.com/madgeek-arc/eosc-observatory/commit/cc0101089cc1ad30acb43989cbcbaa27bf83e43e))
* add status property ([912e89c](https://github.com/madgeek-arc/eosc-observatory/commit/912e89cc91969502197eed3e6e5ca3adcf0a8445))
* adds creation date in thread and returns threads ordered on get methods ([e53902e](https://github.com/madgeek-arc/eosc-observatory/commit/e53902e966a9af4428273f4655de31afb6631380))
* Adds model name in metadata and functionality to regenerate document if different model is set ([e83ab4d](https://github.com/madgeek-arc/eosc-observatory/commit/e83ab4d336db55cdda54d394beaeb857b96a951b))
* adds read/write security methods for documents ([028bffc](https://github.com/madgeek-arc/eosc-observatory/commit/028bffc7d8aaab1124cb5b368c0bc722917b2b65))
* adds status field and authorization in resources registry controller ([8431331](https://github.com/madgeek-arc/eosc-observatory/commit/84313319662529ad2817ad25051cfa5c44344367))
* Adds submission start/close dates to SurveyAnswerInfo. ([efe07fe](https://github.com/madgeek-arc/eosc-observatory/commit/efe07fea1a53efb53be03a9a39257b6bc68c7336))
* adds target to comments and mentions to messages ([6772453](https://github.com/madgeek-arc/eosc-observatory/commit/67724535ff9bdab488308c4b80134eb8c8ea2c54))
* Adds validation in news API request bodies ([e7ce60c](https://github.com/madgeek-arc/eosc-observatory/commit/e7ce60c3ed3aa46858bacccfc82f8a031845b586))
* analytics controller ([ffb9b8e](https://github.com/madgeek-arc/eosc-observatory/commit/ffb9b8e576838ad8db7ab1441cd0095c218c5aae))
* Creates API method to update NewsItem status and active fields. ([a0abb02](https://github.com/madgeek-arc/eosc-observatory/commit/a0abb0240678dc06dc4f70e1aa013196fbd0ddc1))
* creates domain classes and persistence for commenting ([36570d1](https://github.com/madgeek-arc/eosc-observatory/commit/36570d14d7db122fbd2e0dee25163d69034d920f))
* Creates news item delete method for stakeholder members ([4e73c8b](https://github.com/madgeek-arc/eosc-observatory/commit/4e73c8b8c5fc2ab22043e77270a8f19c8e05f479))
* enables authorization when retrieving documents from resources-registry ([7fc4b8a](https://github.com/madgeek-arc/eosc-observatory/commit/7fc4b8a72f3a2331975d0421b7d02ca0b958d94a))
* enables document status changes for admins ([b74b537](https://github.com/madgeek-arc/eosc-observatory/commit/b74b53750336fa143eb24353108568f10fd33061))
* enables filtering private fields from wrapped responses ([7495dd3](https://github.com/madgeek-arc/eosc-observatory/commit/7495dd37189d9734dc1f667b9d42c5065bb620f7))
* Extract survey answer urls and create documents for them ([42776b8](https://github.com/madgeek-arc/eosc-observatory/commit/42776b83c75c5648342758f3f75c9e27aa707560))
* generate documents from urls ([dd55ed0](https://github.com/madgeek-arc/eosc-observatory/commit/dd55ed055283dfa70f2cc5417cbe7cae6a7f6a07))
* Introduces csrf protection ([37124a4](https://github.com/madgeek-arc/eosc-observatory/commit/37124a418e692ea942a52d442e9a6106bf3f9b6f))
* Members of a group are sorted ([c35323d](https://github.com/madgeek-arc/eosc-observatory/commit/c35323d0c81a0b9eca5fbe976a88a1a671c22064))
* Members of a group are sorted ([880c02a](https://github.com/madgeek-arc/eosc-observatory/commit/880c02adace6e358e46db5f32182ff131f6eedf0))
* **news:** Adds NewsItem class and crud methods accessible by admins and stakeholder members. ([48489ed](https://github.com/madgeek-arc/eosc-observatory/commit/48489eda3c76cdadf39d29b9dc9d653a0b7bf9cd))
* Public method returning stakeholder managers' basic information ([3bfeb82](https://github.com/madgeek-arc/eosc-observatory/commit/3bfeb829cc256995c3283d48bda5905adc3cd6ef))
* Public method returning stakeholder managers' basic information ([9b72ceb](https://github.com/madgeek-arc/eosc-observatory/commit/9b72ceb34011505d1a9e473660ef06ccb48e079e))
* replaces old request logging mechanism ([c42f9b2](https://github.com/madgeek-arc/eosc-observatory/commit/c42f9b2279691168ceacc64d1366ed8977ff61fd))
* resources registry controller returns documents with highlights based on keyword search ([7b17fba](https://github.com/madgeek-arc/eosc-observatory/commit/7b17fba9e4fb2eba2338ded56c86a0fb92267af0))
* Resources registry document generation for surveyId ([3511e10](https://github.com/madgeek-arc/eosc-observatory/commit/3511e100d96bb0dbc3529af7df269137af3a3d15))
* **resources-registry,recommendations:** Implements recommendation mechanism for resources-registry ([7a89c50](https://github.com/madgeek-arc/eosc-observatory/commit/7a89c507504f8d6ce174d135f07e70dd2d1d842d))
* returns documents with highlights ([2b3a313](https://github.com/madgeek-arc/eosc-observatory/commit/2b3a3130f5b0e21463dec2cbbadb06daa0875193))
* **tsv:** Create method converting documents to .tsv ([8c07af7](https://github.com/madgeek-arc/eosc-observatory/commit/8c07af7cc21dc0c9fc296f4fb193b10c463d8cae))
* update resources-registry document information ([d18625f](https://github.com/madgeek-arc/eosc-observatory/commit/d18625f290928699b83f1801c85fbdb463847746))
* Updates catalogue dependency ([17b783b](https://github.com/madgeek-arc/eosc-observatory/commit/17b783b09757b601d4dab72d6484f4710ce92daa))
* Vastly improves content of Documents created for websites. ([fc0d2a3](https://github.com/madgeek-arc/eosc-observatory/commit/fc0d2a3e585c6902334108c37ad6075e017d1ec2))


### Bug Fixes

* add authorization to methods generating documents ([11c1537](https://github.com/madgeek-arc/eosc-observatory/commit/11c1537c17f9ddb8df8a8407b49491281c0d8d62))
* add metadata user and status ([2672653](https://github.com/madgeek-arc/eosc-observatory/commit/26726538035ed28f92ba29d3a5842e7e8bece738))
* Adds authorization to non-approved document recommendations ([59b74eb](https://github.com/madgeek-arc/eosc-observatory/commit/59b74eb801d8b799b41d0ffe33705b3a00984826))
* Adds exception handler for failures ([52925ba](https://github.com/madgeek-arc/eosc-observatory/commit/52925ba7145070dc5690427cea97903bf926cfa2))
* adds json annotations to fix comment creation ([2a9b5fc](https://github.com/madgeek-arc/eosc-observatory/commit/2a9b5fc57102de593f7f6116072e4b6d7a7361c7))
* Adds missing @EnableScheduling annotation ([c9c2cec](https://github.com/madgeek-arc/eosc-observatory/commit/c9c2cec546c764ff2e1a77f5012cd4ac37c2fdb0))
* Adds property fixing registry dump ([1fd0329](https://github.com/madgeek-arc/eosc-observatory/commit/1fd0329042fb468865f9739d2ddbddcf62872bda))
* Administrators can retrieve survey answers info ([28c8ae0](https://github.com/madgeek-arc/eosc-observatory/commit/28c8ae06ce253eb6eb744d08a59ae2d99bc7d7fe))
* Allow both GET and POST logout endpoints ([e38b328](https://github.com/madgeek-arc/eosc-observatory/commit/e38b32883bcc136914fd968490e86d7c71131805))
* allow coordinators to update stakeholder users ([8c0515f](https://github.com/madgeek-arc/eosc-observatory/commit/8c0515f0a9d2ed9e27bcbcd6601e2a5a63eb21ba))
* Assigns permission to admins to generate answers ([43fba43](https://github.com/madgeek-arc/eosc-observatory/commit/43fba437ef33e01b8d01c92f824c02905ea40eb8))
* Bumps registry dependency version ([5f9c9b9](https://github.com/madgeek-arc/eosc-observatory/commit/5f9c9b945436d8e4faebcf6e8e4b59d0f0ac34e2))
* Calls save() method when editing a comment ([d6f84c0](https://github.com/madgeek-arc/eosc-observatory/commit/d6f84c0410148bb0744a4cfd2e93c4fc6b995496))
* case sensitive condition resulted in failed invitations ([6ad1fc1](https://github.com/madgeek-arc/eosc-observatory/commit/6ad1fc12a9e71b6ef264bd99ce40c8ac6de2a375))
* Changes user group permissions ([c5fd08a](https://github.com/madgeek-arc/eosc-observatory/commit/c5fd08a01084062067d1efab186a1e208daabc70))
* checks for null mentions ([e3cba66](https://github.com/madgeek-arc/eosc-observatory/commit/e3cba6607c575ba3cafbd914bf70b2572d07c55b))
* compare TypeInfo.getType() == FieldType.composite instead of string equals ([39a9490](https://github.com/madgeek-arc/eosc-observatory/commit/39a949040ba4ca5c5da9e6a5b9bcd5a70f73a454))
* conflict ([b0658f8](https://github.com/madgeek-arc/eosc-observatory/commit/b0658f848c065c73bd2fd63a06ecf5fad67f3284))
* correct configuration for datasets datasource ([8ae1a73](https://github.com/madgeek-arc/eosc-observatory/commit/8ae1a739fbbeb25dca358f2849e470e72a95765b))
* correct request matchers for authorization ([af7e2c4](https://github.com/madgeek-arc/eosc-observatory/commit/af7e2c45587ba3c0f32147d97eea3aa80ccb27a3))
* Correction in security expression ([cae1ccc](https://github.com/madgeek-arc/eosc-observatory/commit/cae1ccc3bdd3f44af6592e8b854208fed9e995a4))
* Corrects canWrite() method ([05e6b4c](https://github.com/madgeek-arc/eosc-observatory/commit/05e6b4cef60e77ea9eb3840dcba0926248431a41))
* corrects condition throwing exception ([024d348](https://github.com/madgeek-arc/eosc-observatory/commit/024d348f30cea756e45bcde468697c3577844d35))
* Corrects security expression in News controller ([5b75b63](https://github.com/madgeek-arc/eosc-observatory/commit/5b75b6324ca53984fa0f22026673344d21dcc8d4))
* corrects targetId type from UUID to String ([be678c6](https://github.com/madgeek-arc/eosc-observatory/commit/be678c65f81f7706f4e63afcff4c10ff1dc76df8))
* Corrects topic sending deleted comment threads ([1835c51](https://github.com/madgeek-arc/eosc-observatory/commit/1835c519a5eebfe49adc81ba1cd79cfc39327fa7))
* Creates cookie with csrf token and enables csrf in swagger-ui ([d8ce636](https://github.com/madgeek-arc/eosc-observatory/commit/d8ce636802070627a701fd2a99ddd303f6a85940))
* Creates metadata class specifically for the resources registry ([e440329](https://github.com/madgeek-arc/eosc-observatory/commit/e44032966219ba059ac012dd97a57b758abe4a2b))
* Deleting comment thread sends to separate topic ([197515b](https://github.com/madgeek-arc/eosc-observatory/commit/197515b7fb273cc04b2a1a7e28ea8ef8aadd2259))
* Document generation by url updates the document's information when it already exists instead of noop ([25d6aee](https://github.com/madgeek-arc/eosc-observatory/commit/25d6aee3b526eda5c589099a765aa02a92979817))
* Ensures that only one answer may be created per survey ([da8675f](https://github.com/madgeek-arc/eosc-observatory/commit/da8675f068d7ddce54e0e6bcff6fa9c079b29266))
* Fixes case where infinite loop might occur ([ffb5666](https://github.com/madgeek-arc/eosc-observatory/commit/ffb5666c68ebced6ad63006f9b60411f1bd992d8))
* Include country/language to document responses ([b45c07a](https://github.com/madgeek-arc/eosc-observatory/commit/b45c07acbcb22e19603bb575e4360f99d638e6ee))
* Include dates in document summary response ([45010fc](https://github.com/madgeek-arc/eosc-observatory/commit/45010fc08dec6876a5be6446dab50b8115817435))
* increase resilience and set paragraphsEn when content already in english ([9480d06](https://github.com/madgeek-arc/eosc-observatory/commit/9480d06a4b2df09cd776dd6cb91c1e9cb98dc790))
* log errors from fire-and-forget post-thread-update tasks ([d112a52](https://github.com/madgeek-arc/eosc-observatory/commit/d112a527462f6494329c403928f5e4fe0f2cb2ac))
* makes GET calls in forms controller public ([27c4bff](https://github.com/madgeek-arc/eosc-observatory/commit/27c4bff7c724062a51ce01f93a091bd56073a7dc))
* null-guard user id/email lookups in survey history and CSV export ([de60b25](https://github.com/madgeek-arc/eosc-observatory/commit/de60b252a07d777d1ddca0fa54a5992239a15d26))
* Override equals and hashCode methods ([8913d3b](https://github.com/madgeek-arc/eosc-observatory/commit/8913d3b25f20ce77b73ed63a207fd90706b10416))
* pageviews now returns correct number of months ([2ef7831](https://github.com/madgeek-arc/eosc-observatory/commit/2ef783105650bb5ad9bef7ee6fdfddbc3779f34a))
* permit STOMP DISCONNECT frames to avoid Spring Security access denied warnings ([8b1c098](https://github.com/madgeek-arc/eosc-observatory/commit/8b1c098a872fd0b7ef88dc0adced1ff037526737))
* Public method returning stakeholder managers ignores users with missing name and surname ([76e6dcf](https://github.com/madgeek-arc/eosc-observatory/commit/76e6dcf382c9f64fdebd3cd177e4bd40d6f5c9fc))
* Public method returning stakeholder managers ignores users with missing name and surname ([5e17fe5](https://github.com/madgeek-arc/eosc-observatory/commit/5e17fe5d842a7557937e6ecc13e865735b987ada))
* reintroduced 'userId' alongside 'editors' for backward compatibility ([76e9690](https://github.com/madgeek-arc/eosc-observatory/commit/76e9690eed50408d6797d574e3f2a825e802626b))
* remove deprecated fields from exported csv and fix list of editors ([7911f0a](https://github.com/madgeek-arc/eosc-observatory/commit/7911f0a70bc12d8710105254c773b1dbf187dc4b))
* remove extension of Logs Controller and properly handle authentication in filter chain ([f2c15ab](https://github.com/madgeek-arc/eosc-observatory/commit/f2c15ab47f07a787bb98326f119b8808830ac5d9))
* replace deprecated configuration ([71d0c66](https://github.com/madgeek-arc/eosc-observatory/commit/71d0c663bf51fe4d93428ff0cb983034111efa0e))
* replace impl class with interface ([d0d973e](https://github.com/madgeek-arc/eosc-observatory/commit/d0d973eb171a610c42eda07a7172320b6c7f9f50))
* replaced deprecated usage of 'userId' with 'editors' field when creating public metadata ([57cbc31](https://github.com/madgeek-arc/eosc-observatory/commit/57cbc317169004ec8fec602084a4b5ab527633d3))
* restore redis configuration ([09e510b](https://github.com/madgeek-arc/eosc-observatory/commit/09e510b9edabb9ef44ca89acdd54b9b444630c09))
* Reuse existing survey answers when generate method finds them ([27c2731](https://github.com/madgeek-arc/eosc-observatory/commit/27c2731baf90123b357cc08d23819d9f45fa4da8))
* Secures generic controller ([a3884a9](https://github.com/madgeek-arc/eosc-observatory/commit/a3884a9c10660a0bc5428bd767d773f0394910d8))
* Secures methods retrieving documents by id and migrating to another resource type ([356ff88](https://github.com/madgeek-arc/eosc-observatory/commit/356ff88ef389212bbd4e437c37bfee98e2a5a282))
* Secures update/delete actions on comments ([4aa5949](https://github.com/madgeek-arc/eosc-observatory/commit/4aa5949020315bb686bbb900fb60e9c922bf7999))
* security configuration ([fca03a8](https://github.com/madgeek-arc/eosc-observatory/commit/fca03a87f7c3c610e7394eee020bde8d2bc90b11))
* **security:** remove auth log ([6ca5e05](https://github.com/madgeek-arc/eosc-observatory/commit/6ca5e05aaffa4cf22b087e9e47a3d2cd36ec5b5f))
* **security:** Removes access token from cookie ([0feccf9](https://github.com/madgeek-arc/eosc-observatory/commit/0feccf90eac7fb9f27cd5c953439816d4ea15be5))
* set bean primary to avoid autoconfiguration conflicts ([0d4e4be](https://github.com/madgeek-arc/eosc-observatory/commit/0d4e4be07fec0716bebc229bb20d113fbd0c7eb8))
* Set fileType to empty string if null ([b64a241](https://github.com/madgeek-arc/eosc-observatory/commit/b64a24175bf70f0c183313854d762cd0e5f4f4b1))
* spel expression ([df78e43](https://github.com/madgeek-arc/eosc-observatory/commit/df78e43c493133d2d0c486fa0d2672cf54640099))
* **stakeholders:** fix authorization spel when retrieving stakeholders ([b11cad8](https://github.com/madgeek-arc/eosc-observatory/commit/b11cad805423a284f6b147b6b22ecece95f31c60))
* status and source ([777c325](https://github.com/madgeek-arc/eosc-observatory/commit/777c3255905b866bcf3a5d81713bf9985cd43101))
* throw error instead of hiding it ([8d72803](https://github.com/madgeek-arc/eosc-observatory/commit/8d728035e1109bb52cb83809e3c8e384ab435c21))
* update user sub if changed ([2b4cf70](https://github.com/madgeek-arc/eosc-observatory/commit/2b4cf703417b860318e98b5951ea8e8c161c6474))
* Updates registry dependency fixing issue of lazy initialization of resource type aliases - removed workaround properties ([3da55db](https://github.com/madgeek-arc/eosc-observatory/commit/3da55dbbe302cee838d67cfebcea1d9c5db4c8cf))
* Updates registry dependency fixing issue of lazy initialization of resource type aliases - removed workaround properties ([753fe00](https://github.com/madgeek-arc/eosc-observatory/commit/753fe0011a5e3068fb1bdaabc645368269e2b0e5))
* Updates resources-registry document generation template ([0dd3149](https://github.com/madgeek-arc/eosc-observatory/commit/0dd3149e448f2d1239754904eee2e6e29b1e3137))
* Updating a document sets 'curated' flag to true ([7059b69](https://github.com/madgeek-arc/eosc-observatory/commit/7059b69426a7c1fb7097a4d94ea8a26b2a942bc4))
* Updating a document sets 'curated' flag to true ([e7fd0d3](https://github.com/madgeek-arc/eosc-observatory/commit/e7fd0d3ecb255111680a56db89dee4ee771273bd))
* Updating a message method uses findWithCommentById() method which initializes the comment thread - fixes lazy initialization outside of session when mapping to dto ([ba39258](https://github.com/madgeek-arc/eosc-observatory/commit/ba39258da2ccaceb1096d6d629d364aff20ad48f))
* use setter to set email so it will be lowercased ([23183ca](https://github.com/madgeek-arc/eosc-observatory/commit/23183ca2008be23cf564ff89fbaa28071b63e3c4))
* When user is missing from db assigns 'unknown' to the fullname ([e439d38](https://github.com/madgeek-arc/eosc-observatory/commit/e439d380e433c1b79ee3276b012783b355f2e7c2))


### Performance Improvements

* **analytics,pageviews:** Changes method to request pageviews per month instead of per day ([57c42ad](https://github.com/madgeek-arc/eosc-observatory/commit/57c42ad69dca84842e674d0c5aa75b4948ea132a))
* **answers-info:** Pre-fetches models and stakeholders to reduce DB calls. ([f966f5f](https://github.com/madgeek-arc/eosc-observatory/commit/f966f5fc61d422b92d6cf0d4ba98e20bae5d557d))
* Creates cut-down versions of the resources-registry Document to reduce data transfer size and speed on get calls ([28fc4cf](https://github.com/madgeek-arc/eosc-observatory/commit/28fc4cf50ccf14b8f12f3ff7118d9e2e6610848d))
* Document recommendations endpoint returns summaries of documents by default ([2222b7a](https://github.com/madgeek-arc/eosc-observatory/commit/2222b7ac4938bb2a06089929f4ffac1259e778a0))
* Replaces synchronized method with more fine-grained resource locking. ([78d34e4](https://github.com/madgeek-arc/eosc-observatory/commit/78d34e4dff164b0a53b9fbc74118b414d688f2ee))


### Documentation

* add license notice to files ([da17049](https://github.com/madgeek-arc/eosc-observatory/commit/da17049f12db24d75f01bcf946b95f7b609bc4b9))


### Miscellaneous Chores

* release 4.3.0 ([9bac44e](https://github.com/madgeek-arc/eosc-observatory/commit/9bac44e3114a2a98c8fddb7a64b5e046e827480c))


### Code Refactoring

* Change API responses to json ([8d86d7f](https://github.com/madgeek-arc/eosc-observatory/commit/8d86d7f9d5bb520229ef0fb657f521d6a92a1e35))
* change group id and package names ([484b78d](https://github.com/madgeek-arc/eosc-observatory/commit/484b78de1290c1656e2c572c5c54bb15e244a53a))

## [4.4.2](https://github.com/madgeek-arc/eosc-observatory/compare/4.4.1...4.4.2) (2026-09-18)


### Bug Fixes

* log errors from fire-and-forget post-thread-update tasks ([21eea3b](https://github.com/madgeek-arc/eosc-observatory/commit/21eea3b8783bd816083d05eabfce7b0c24d48422))

## [4.4.1](https://github.com/madgeek-arc/eosc-observatory/compare/4.4.0...4.4.1) (2026-09-11)


### Bug Fixes

* allow coordinators to update stakeholder users ([8c0515f](https://github.com/madgeek-arc/eosc-observatory/commit/8c0515f0a9d2ed9e27bcbcd6601e2a5a63eb21ba))
* compare TypeInfo.getType() == FieldType.composite instead of string equals ([ad7e201](https://github.com/madgeek-arc/eosc-observatory/commit/ad7e2017ec1c3cd03ec7b5d26bc7d14cec6b506c))
* null-guard user id/email lookups in survey history and CSV export ([de60b25](https://github.com/madgeek-arc/eosc-observatory/commit/de60b252a07d777d1ddca0fa54a5992239a15d26))
* throw error instead of hiding it ([8d72803](https://github.com/madgeek-arc/eosc-observatory/commit/8d728035e1109bb52cb83809e3c8e384ab435c21))
* use setter to set email so it will be lowercased ([23183ca](https://github.com/madgeek-arc/eosc-observatory/commit/23183ca2008be23cf564ff89fbaa28071b63e3c4))

## [4.4.0](https://github.com/madgeek-arc/eosc-observatory/compare/4.3.0...4.4.0) (2026-05-21)


### Features

* add method returning public news of a stakeholder ([dd6e599](https://github.com/madgeek-arc/eosc-observatory/commit/dd6e59945d28ac5aa923a54baaf9f02324f2ea01))
* add method returning views for the provided path ([d7add13](https://github.com/madgeek-arc/eosc-observatory/commit/d7add13aa050f747d0d782b9dd02272d8089f818))


### Bug Fixes

* pageviews now returns correct number of months ([2ef7831](https://github.com/madgeek-arc/eosc-observatory/commit/2ef783105650bb5ad9bef7ee6fdfddbc3779f34a))
* permit STOMP DISCONNECT frames to avoid Spring Security access denied warnings ([8b1c098](https://github.com/madgeek-arc/eosc-observatory/commit/8b1c098a872fd0b7ef88dc0adced1ff037526737))

## [4.3.0](https://github.com/madgeek-arc/eosc-observatory/compare/4.2.1...4.3.0) (2026-04-01)


### Features

* Adds validation in news API request bodies ([e7ce60c](https://github.com/madgeek-arc/eosc-observatory/commit/e7ce60c3ed3aa46858bacccfc82f8a031845b586))
* Creates API method to update NewsItem status and active fields. ([a0abb02](https://github.com/madgeek-arc/eosc-observatory/commit/a0abb0240678dc06dc4f70e1aa013196fbd0ddc1))
* Creates news item delete method for stakeholder members ([4e73c8b](https://github.com/madgeek-arc/eosc-observatory/commit/4e73c8b8c5fc2ab22043e77270a8f19c8e05f479))
* **news:** Adds NewsItem class and crud methods accessible by admins and stakeholder members. ([48489ed](https://github.com/madgeek-arc/eosc-observatory/commit/48489eda3c76cdadf39d29b9dc9d653a0b7bf9cd))


### Bug Fixes

* Adds missing @EnableScheduling annotation ([c9c2cec](https://github.com/madgeek-arc/eosc-observatory/commit/c9c2cec546c764ff2e1a77f5012cd4ac37c2fdb0))
* Allow both GET and POST logout endpoints ([e38b328](https://github.com/madgeek-arc/eosc-observatory/commit/e38b32883bcc136914fd968490e86d7c71131805))
* Corrects canWrite() method ([05e6b4c](https://github.com/madgeek-arc/eosc-observatory/commit/05e6b4cef60e77ea9eb3840dcba0926248431a41))
* Corrects security expression in News controller ([5b75b63](https://github.com/madgeek-arc/eosc-observatory/commit/5b75b6324ca53984fa0f22026673344d21dcc8d4))
* Ensures that only one answer may be created per survey ([da8675f](https://github.com/madgeek-arc/eosc-observatory/commit/da8675f068d7ddce54e0e6bcff6fa9c079b29266))
* Override equals and hashCode methods ([8913d3b](https://github.com/madgeek-arc/eosc-observatory/commit/8913d3b25f20ce77b73ed63a207fd90706b10416))
* Reuse existing survey answers when generate method finds them ([27c2731](https://github.com/madgeek-arc/eosc-observatory/commit/27c2731baf90123b357cc08d23819d9f45fa4da8))
* When user is missing from db assigns 'unknown' to the fullname ([e439d38](https://github.com/madgeek-arc/eosc-observatory/commit/e439d380e433c1b79ee3276b012783b355f2e7c2))


### Performance Improvements

* **analytics,pageviews:** Changes method to request pageviews per month instead of per day ([57c42ad](https://github.com/madgeek-arc/eosc-observatory/commit/57c42ad69dca84842e674d0c5aa75b4948ea132a))
* Replaces synchronized method with more fine-grained resource locking. ([78d34e4](https://github.com/madgeek-arc/eosc-observatory/commit/78d34e4dff164b0a53b9fbc74118b414d688f2ee))


### Miscellaneous Chores

* release 4.3.0 ([9bac44e](https://github.com/madgeek-arc/eosc-observatory/commit/9bac44e3114a2a98c8fddb7a64b5e046e827480c))

## [4.0.0](https://github.com/madgeek-arc/eosc-observatory/compare/3.2.0...4.0.0) (2025-12-23)

### ⚠ BREAKING CHANGES

* Changes API responses to json

### Features

* add most informative paragraphs to Document ([cc01010](https://github.com/madgeek-arc/eosc-observatory/commit/cc0101089cc1ad30acb43989cbcbaa27bf83e43e))
* add status property ([912e89c](https://github.com/madgeek-arc/eosc-observatory/commit/912e89cc91969502197eed3e6e5ca3adcf0a8445))
* adds read/write security methods for documents ([028bffc](https://github.com/madgeek-arc/eosc-observatory/commit/028bffc7d8aaab1124cb5b368c0bc722917b2b65))
* adds status field and authorization in resources registry controller ([8431331](https://github.com/madgeek-arc/eosc-observatory/commit/84313319662529ad2817ad25051cfa5c44344367))
* enables document status changes for admins ([b74b537](https://github.com/madgeek-arc/eosc-observatory/commit/b74b53750336fa143eb24353108568f10fd33061))
* enables filtering private fields from wrapped responses ([7495dd3](https://github.com/madgeek-arc/eosc-observatory/commit/7495dd37189d9734dc1f667b9d42c5065bb620f7))
* Extract survey answer urls and create documents for them ([42776b8](https://github.com/madgeek-arc/eosc-observatory/commit/42776b83c75c5648342758f3f75c9e27aa707560))
* generate documents from urls ([dd55ed0](https://github.com/madgeek-arc/eosc-observatory/commit/dd55ed055283dfa70f2cc5417cbe7cae6a7f6a07))
* replaces old request logging mechanism ([c42f9b2](https://github.com/madgeek-arc/eosc-observatory/commit/c42f9b2279691168ceacc64d1366ed8977ff61fd))
* resources registry controller returns documents with highlights based on keyword search ([7b17fba](https://github.com/madgeek-arc/eosc-observatory/commit/7b17fba9e4fb2eba2338ded56c86a0fb92267af0))
* returns documents with highlights ([2b3a313](https://github.com/madgeek-arc/eosc-observatory/commit/2b3a3130f5b0e21463dec2cbbadb06daa0875193))
* **tsv:** Create method converting documents to .tsv ([8c07af7](https://github.com/madgeek-arc/eosc-observatory/commit/8c07af7cc21dc0c9fc296f4fb193b10c463d8cae))
* update resources-registry document information ([d18625f](https://github.com/madgeek-arc/eosc-observatory/commit/d18625f290928699b83f1801c85fbdb463847746))

### Bug Fixes

* add authorization to methods generating documents ([11c1537](https://github.com/madgeek-arc/eosc-observatory/commit/11c1537c17f9ddb8df8a8407b49491281c0d8d62))
* add metadata user and status ([2672653](https://github.com/madgeek-arc/eosc-observatory/commit/26726538035ed28f92ba29d3a5842e7e8bece738))
* corrects condition throwing exception ([024d348](https://github.com/madgeek-arc/eosc-observatory/commit/024d348f30cea756e45bcde468697c3577844d35))
* increase resilience and set paragraphsEn when content already in english ([9480d06](https://github.com/madgeek-arc/eosc-observatory/commit/9480d06a4b2df09cd776dd6cb91c1e9cb98dc790))
* makes GET calls in forms controller public ([27c4bff](https://github.com/madgeek-arc/eosc-observatory/commit/27c4bff7c724062a51ce01f93a091bd56073a7dc))
* **security:** remove auth log ([6ca5e05](https://github.com/madgeek-arc/eosc-observatory/commit/6ca5e05aaffa4cf22b087e9e47a3d2cd36ec5b5f))
* status and source ([777c325](https://github.com/madgeek-arc/eosc-observatory/commit/777c3255905b866bcf3a5d81713bf9985cd43101))

### Code Refactoring

* Change API responses to json ([8d86d7f](https://github.com/madgeek-arc/eosc-observatory/commit/8d86d7f9d5bb520229ef0fb657f521d6a92a1e35))

## [3.2.0](https://github.com/madgeek-arc/eosc-observatory/compare/3.1.0...3.2.0) (2025-07-03)

### Bug Fixes

* update user sub if changed ([2b4cf70](https://github.com/madgeek-arc/eosc-observatory/commit/2b4cf703417b860318e98b5951ea8e8c161c6474))

## [3.1.0](https://github.com/madgeek-arc/eosc-observatory/compare/3.0.0...3.1.0) (2025-06-16)

## [3.0.0](https://github.com/madgeek-arc/eosc-observatory/compare/2.8.4...3.0.0) (2025-02-18)

### ⚠ BREAKING CHANGES

* change group id and package names
* 1. Incompatible Flyway migrations introduced by 'registry' dependency.
Database must be backed up and migrated manually.
2. Resource Types' 'indexMapperClass' field must be manually changed to 'gr.uoa.di.madgik.registry.index.DefaultIndexMapper'. A dump-restore procedure is suggested.

* build(java,spring-boot,registry,catalogue)\!: bump java, spring-boot, registry, catalogue versions ([7c8f377](https://github.com/madgeek-arc/eosc-observatory/commit/7c8f3775216468144fde578817b9419307ad74a7))

### Features

* remove deprecated fields when importing from previous year ([d1f5c46](https://github.com/madgeek-arc/eosc-observatory/commit/d1f5c466b270e4c87f512f4e50ae783673fea5e2))

### Bug Fixes

* correct configuration for datasets datasource ([8ae1a73](https://github.com/madgeek-arc/eosc-observatory/commit/8ae1a739fbbeb25dca358f2849e470e72a95765b))
* correct request matchers for authorization ([af7e2c4](https://github.com/madgeek-arc/eosc-observatory/commit/af7e2c45587ba3c0f32147d97eea3aa80ccb27a3))
* remove deprecated fields from exported csv and fix list of editors ([7911f0a](https://github.com/madgeek-arc/eosc-observatory/commit/7911f0a70bc12d8710105254c773b1dbf187dc4b))
* remove extension of Logs Controller and properly handle authentication in filter chain ([f2c15ab](https://github.com/madgeek-arc/eosc-observatory/commit/f2c15ab47f07a787bb98326f119b8808830ac5d9))
* replace deprecated configuration ([71d0c66](https://github.com/madgeek-arc/eosc-observatory/commit/71d0c663bf51fe4d93428ff0cb983034111efa0e))
* replace impl class with interface ([d0d973e](https://github.com/madgeek-arc/eosc-observatory/commit/d0d973eb171a610c42eda07a7172320b6c7f9f50))
* restore redis configuration ([09e510b](https://github.com/madgeek-arc/eosc-observatory/commit/09e510b9edabb9ef44ca89acdd54b9b444630c09))
* security configuration ([fca03a8](https://github.com/madgeek-arc/eosc-observatory/commit/fca03a87f7c3c610e7394eee020bde8d2bc90b11))
* set bean primary to avoid autoconfiguration conflicts ([0d4e4be](https://github.com/madgeek-arc/eosc-observatory/commit/0d4e4be07fec0716bebc229bb20d113fbd0c7eb8))
* spel expression ([df78e43](https://github.com/madgeek-arc/eosc-observatory/commit/df78e43c493133d2d0c486fa0d2672cf54640099))
* **stakeholders:** fix authorization spel when retrieving stakeholders ([b11cad8](https://github.com/madgeek-arc/eosc-observatory/commit/b11cad805423a284f6b147b6b22ecece95f31c60))

### Reverts

* Revert "refactor: add qualifier for jpa properties" ([ce3370b](https://github.com/madgeek-arc/eosc-observatory/commit/ce3370babb5443b7b356d555d320fb8fbeb36cbe))

### Code Refactoring

* change group id and package names ([484b78d](https://github.com/madgeek-arc/eosc-observatory/commit/484b78de1290c1656e2c572c5c54bb15e244a53a))
