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
