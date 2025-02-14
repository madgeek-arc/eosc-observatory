##  (2025-02-14)

### ⚠ BREAKING CHANGES

* Change group id and package names
* Bump java, spring-boot, registry, catalogue versions ([7c8f377](https://github.com/madgeek-arc/eosc-observatory/commit/7c8f3775216468144fde578817b9419307ad74a7))
  Resource Types' 'indexMapperClass' field must be manually changed to 'gr.uoa.di.madgik.registry.index.DefaultIndexMapper'.

### Features

* Remove deprecated fields when importing from previous year ([d1f5c46](https://github.com/madgeek-arc/eosc-observatory/commit/d1f5c466b270e4c87f512f4e50ae783673fea5e2))

### Bug Fixes

* Correct request matchers for authorization ([af7e2c4](https://github.com/madgeek-arc/eosc-observatory/commit/af7e2c45587ba3c0f32147d97eea3aa80ccb27a3))
* Remove deprecated fields from exported csv and fix list of editors ([7911f0a](https://github.com/madgeek-arc/eosc-observatory/commit/7911f0a70bc12d8710105254c773b1dbf187dc4b))
* Remove extension of Logs Controller and properly handle authentication in filter chain ([f2c15ab](https://github.com/madgeek-arc/eosc-observatory/commit/f2c15ab47f07a787bb98326f119b8808830ac5d9))
* Replace deprecated configuration ([71d0c66](https://github.com/madgeek-arc/eosc-observatory/commit/71d0c663bf51fe4d93428ff0cb983034111efa0e))
* Restore redis configuration ([09e510b](https://github.com/madgeek-arc/eosc-observatory/commit/09e510b9edabb9ef44ca89acdd54b9b444630c09))
* Security configuration ([fca03a8](https://github.com/madgeek-arc/eosc-observatory/commit/fca03a87f7c3c610e7394eee020bde8d2bc90b11))
* Set bean primary to avoid autoconfiguration conflicts ([0d4e4be](https://github.com/madgeek-arc/eosc-observatory/commit/0d4e4be07fec0716bebc229bb20d113fbd0c7eb8))
* spel expression ([df78e43](https://github.com/madgeek-arc/eosc-observatory/commit/df78e43c493133d2d0c486fa0d2672cf54640099))
* **stakeholders:** fix authorization spel when retrieving stakeholders ([b11cad8](https://github.com/madgeek-arc/eosc-observatory/commit/b11cad805423a284f6b147b6b22ecece95f31c60))

### Code Refactoring

* Change group id and package names ([484b78d](https://github.com/madgeek-arc/eosc-observatory/commit/484b78de1290c1656e2c572c5c54bb15e244a53a))
