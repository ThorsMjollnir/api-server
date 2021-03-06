WITH t AS (
    SELECT code, COALESCE(t1.local_description, t2.local_description) AS local_description
      FROM foods
        LEFT JOIN foods_local as t1 ON foods.code = t1.food_code AND t1.locale_id = {locale_id}
        LEFT JOIN foods_local as t2 ON foods.code = t2.food_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})
        LEFT JOIN foods_restrictions ON foods.code = foods_restrictions.food_code
      WHERE 
        (t1.local_description IS NOT NULL OR t2.local_description IS NOT NULL)
        AND NOT (COALESCE(t1.do_not_use, t2.do_not_use, false))
        AND (foods_restrictions.locale_id = {locale_id} OR foods_restrictions.locale_id IS NULL)
    )
SELECT t.code, t.local_description FROM
  t 
    JOIN foods_categories as fc ON t.code = fc.food_code
    JOIN categories as cats ON cats.code = fc.category_code
GROUP BY (t.code, t.local_description)
HAVING (NOT bool_and(cats.is_hidden))
