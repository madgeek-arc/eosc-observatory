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
