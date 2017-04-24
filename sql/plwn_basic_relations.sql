ALTER TABLE `plwn_basic`.`literals` 
ADD PRIMARY KEY (`id`);

ALTER TABLE `plwn_basic`.`synsets` 
ADD PRIMARY KEY (`id`);

ALTER TABLE `plwn_basic`.`synset_literals` 
ADD PRIMARY KEY (`id_literal`, `id_synset`),
ADD INDEX `fk_synset_sl_idx` (`id_synset` ASC);
ALTER TABLE `plwn_basic`.`synset_literals` 
ADD CONSTRAINT `fk_synset_sl`
  FOREIGN KEY (`id_synset`)
  REFERENCES `plwn_basic`.`synsets` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `fk_literal_sl`
  FOREIGN KEY (`id_literal`)
  REFERENCES `plwn_basic`.`literals` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `plwn_basic`.`relations` 
ADD PRIMARY KEY (`id`),
ADD INDEX `fk_synset_pr_idx` (`synset_id_p` ASC),
ADD INDEX `fk_synset_cr_idx` (`synset_id_c` ASC);
ALTER TABLE `plwn_basic`.`relations` 
ADD CONSTRAINT `fk_synset_pr`
  FOREIGN KEY (`synset_id_p`)
  REFERENCES `plwn_basic`.`synsets` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `fk_synset_cr`
  FOREIGN KEY (`synset_id_c`)
  REFERENCES `plwn_basic`.`synsets` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

